package com.cashflow.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.CategoryExpense
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.FinancialInsight
import com.cashflow.ai.domain.model.MonthlyClose
import com.cashflow.ai.domain.model.MonthlyTotal
import com.cashflow.ai.domain.model.ParsedQuickTransaction
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSummary
import com.cashflow.ai.domain.repository.TransactionRepository
import com.cashflow.ai.domain.usecase.GenerateFinancialInsightUseCase
import com.cashflow.ai.domain.usecase.ai.ParseNaturalLanguageTransactionsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val selectedDateRange: DateRange = DateRange.THIS_MONTH,
    val summary: TransactionSummary = TransactionSummary(),
    val categoryExpenses: List<CategoryExpense> = emptyList(),
    val monthlyTrends: List<MonthlyTotal> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val insight: FinancialInsight? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isGeneratingInsight: Boolean = false,
    val chatInputText: String = "",
    val isParsingChatInput: Boolean = false,
    val parsedBatchTransactions: List<ParsedQuickTransaction> = emptyList(),
    val isBatchReviewSheetOpen: Boolean = false,
    val snackbarMessage: String? = null
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val parseNaturalLanguageTransactionsUseCase: ParseNaturalLanguageTransactionsUseCase? = null
) : ViewModel() {

    private val _selectedDateRange = MutableStateFlow(DateRange.THIS_MONTH)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isGeneratingInsight = MutableStateFlow(false)
    private val _chatInputText = MutableStateFlow("")
    private val _isParsingChatInput = MutableStateFlow(false)
    private val _parsedBatchTransactions = MutableStateFlow<List<ParsedQuickTransaction>>(emptyList())
    private val _isBatchReviewSheetOpen = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    private val insightGenerator = GenerateFinancialInsightUseCase()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = _selectedDateRange.flatMapLatest { dateRange ->
        val summaryFlow = transactionRepository.getSummary(dateRange)
        val categoryExpensesFlow = transactionRepository.getCategoryExpenses(dateRange)
        val monthlyTrendsFlow = transactionRepository.getMonthlyTrends(DateRange.LAST_6_MONTHS)
        val recentTransactionsFlow = transactionRepository.getTransactions(dateRange = dateRange)
        val categoriesFlow = transactionRepository.getAllCategories()
        val latestCloseFlow = transactionRepository.getLatestMonthlyClose()

        combine(
            summaryFlow,
            categoryExpensesFlow,
            monthlyTrendsFlow,
            recentTransactionsFlow,
            categoriesFlow,
            _chatInputText,
            _isParsingChatInput,
            _parsedBatchTransactions,
            _isBatchReviewSheetOpen,
            _snackbarMessage
        ) { args: Array<Any?> ->
            val summary = args[0] as TransactionSummary
            val categoryExpenses = args[1] as List<CategoryExpense>
            val monthlyTrends = args[2] as List<MonthlyTotal>
            val transactions = args[3] as List<Transaction>
            val categories = args[4] as List<Category>
            val chatText = args[5] as String
            val isParsing = args[6] as Boolean
            val parsedBatch = args[7] as List<ParsedQuickTransaction>
            val isBatchOpen = args[8] as Boolean
            val snackbar = args[9] as? String

            DashboardUiState(
                selectedDateRange = dateRange,
                summary = summary,
                categoryExpenses = categoryExpenses,
                monthlyTrends = monthlyTrends,
                recentTransactions = transactions.take(5),
                categories = categories,
                isLoading = false,
                isRefreshing = _isRefreshing.value,
                isGeneratingInsight = _isGeneratingInsight.value,
                chatInputText = chatText,
                isParsingChatInput = isParsing,
                parsedBatchTransactions = parsedBatch,
                isBatchReviewSheetOpen = isBatchOpen,
                snackbarMessage = snackbar
            )
        }.combine(latestCloseFlow) { state, latestClose ->
            state.copy(insight = latestClose?.toInsight())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun onDateRangeChanged(newRange: DateRange) {
        _selectedDateRange.value = newRange
    }

    fun onChatInputChanged(newText: String) {
        _chatInputText.value = newText
    }

    fun parseChatInput() {
        val text = _chatInputText.value.trim()
        if (text.isBlank() || _isParsingChatInput.value) return

        viewModelScope.launch {
            _isParsingChatInput.value = true
            val parsedList = if (parseNaturalLanguageTransactionsUseCase != null) {
                parseNaturalLanguageTransactionsUseCase(text)
            } else {
                emptyList()
            }
            _isParsingChatInput.value = false

            if (parsedList.isNotEmpty()) {
                _parsedBatchTransactions.value = parsedList
                _isBatchReviewSheetOpen.value = true
            } else {
                _snackbarMessage.value = "Could not parse amount from your input. Try e.g. 'bensin 30k, makan 50rb'"
            }
        }
    }

    fun dismissBatchReviewSheet() {
        _isBatchReviewSheetOpen.value = false
    }

    fun saveBatchTransactions(items: List<ParsedQuickTransaction>) {
        if (items.isEmpty()) return

        viewModelScope.launch {
            val domainTransactions = items.map { it.toTransaction() }
            val result = transactionRepository.insertTransactions(domainTransactions)
            if (result.isSuccess) {
                _isBatchReviewSheetOpen.value = false
                _chatInputText.value = ""
                _parsedBatchTransactions.value = emptyList()
                _snackbarMessage.value = "Saved ${items.size} transaction${if (items.size > 1) "s" else ""} successfully! 🎉"
            } else {
                _snackbarMessage.value = "Failed to save transactions: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun generateInsight() {
        viewModelScope.launch {
            _isGeneratingInsight.value = true
            val summary = transactionRepository.getSummary(_selectedDateRange.value).first()
            val categories = transactionRepository.getCategoryExpenses(_selectedDateRange.value).first()
            val insight = insightGenerator(
                period = _selectedDateRange.value.name,
                summary = summary,
                categoryExpenses = categories
            )
            transactionRepository.saveMonthlyClose(
                MonthlyClose(
                    month = DateUtils.today().substring(0, 7),
                    income = summary.totalIncome,
                    expense = summary.totalExpense,
                    net = summary.netBalance,
                    topExpenseCategory = categories.firstOrNull()?.category,
                    insight = "${insight.headline}\n${insight.body}",
                    isAiGenerated = false
                )
            )
            _isGeneratingInsight.value = false
        }
    }

    private fun MonthlyClose.toInsight() = FinancialInsight(
        period = month,
        headline = insight.substringBefore('\n').ifBlank { "Your financial insight" },
        recommendations = insight.substringAfter('\n', "").split(". ")
            .filter { it.isNotBlank() }
            .map { if (it.endsWith('.')) it else "$it." },
        generatedAt = generatedAt,
        isAiGenerated = isAiGenerated
    )

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val parseNaturalLanguageTransactionsUseCase: ParseNaturalLanguageTransactionsUseCase? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(transactionRepository, parseNaturalLanguageTransactionsUseCase) as T
        }
    }
}
