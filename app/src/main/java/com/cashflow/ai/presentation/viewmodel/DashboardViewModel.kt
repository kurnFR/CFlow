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
    val customStartDate: String? = null,
    val customEndDate: String? = null,
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
    private val _customStartDate = MutableStateFlow<String?>(null)
    private val _customEndDate = MutableStateFlow<String?>(null)
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
        val summaryFlow = when {
            dateRange == DateRange.CUSTOM -> transactionRepository.getSummary(
                dateRange = dateRange,
                startDate = _customStartDate.value,
                endDate = _customEndDate.value
            )
            else -> transactionRepository.getSummary(dateRange)
        }
        val categoryExpensesFlow = when {
            dateRange == DateRange.CUSTOM -> transactionRepository.getCategoryExpenses(
                dateRange = dateRange,
                startDate = _customStartDate.value,
                endDate = _customEndDate.value
            )
            else -> transactionRepository.getCategoryExpenses(dateRange)
        }
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
            _snackbarMessage,
            _customStartDate,
            _customEndDate
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
            val customStart = args[10] as? String
            val customEnd = args[11] as? String

            DashboardUiState(
                selectedDateRange = dateRange,
                customStartDate = customStart,
                customEndDate = customEnd,
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
        if (newRange != DateRange.CUSTOM) {
            _customStartDate.value = null
            _customEndDate.value = null
        }
    }

    fun onCustomDateRangeSelected(startDate: String, endDate: String) {
        _customStartDate.value = startDate
        _customEndDate.value = endDate
        _selectedDateRange.value = DateRange.CUSTOM
    }

    fun onChatInputChanged(newText: String) {
        _chatInputText.value = newText
    }

    fun parseChatInput() {
        val text = _chatInputText.value.trim()
        if (text.isBlank() || _isParsingChatInput.value) return

        viewModelScope.launch {
            _isParsingChatInput.value = true
            val parsedList = parseNaturalLanguageTransactionsUseCase?.invoke(text) ?: emptyList()
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
            try {
                val selectedRange = _selectedDateRange.value
                val summary = when (selectedRange) {
                    DateRange.CUSTOM -> transactionRepository.getSummary(
                        dateRange = selectedRange,
                        startDate = _customStartDate.value,
                        endDate = _customEndDate.value
                    ).first()
                    else -> transactionRepository.getSummary(selectedRange).first()
                }
                val categories = when (selectedRange) {
                    DateRange.CUSTOM -> transactionRepository.getCategoryExpenses(
                        dateRange = selectedRange,
                        startDate = _customStartDate.value,
                        endDate = _customEndDate.value
                    ).first()
                    else -> transactionRepository.getCategoryExpenses(selectedRange).first()
                }
                val insight = insightGenerator(
                    period = selectedRange.name,
                    summary = summary,
                    categoryExpenses = categories
                )
                transactionRepository.saveMonthlyClose(
                    MonthlyClose(
                        month = DateUtils.today().substring(0, 7),
                        income = summary.totalIncome,
                        expense = summary.totalExpense,
                        net = summary.netBalance,
                        topExpenseCategory = categories.maxByOrNull { it.total }?.category,
                        insight = "${insight.headline}\n${insight.body}",
                        isAiGenerated = true,
                        generatedAt = System.currentTimeMillis()
                    )
                )
                _snackbarMessage.value = "AI insight refreshed for ${selectedRange.name.lowercase().replace('_', ' ')}."
            } catch (e: Exception) {
                _snackbarMessage.value = "Could not refresh AI insight: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isGeneratingInsight.value = false
            }
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
