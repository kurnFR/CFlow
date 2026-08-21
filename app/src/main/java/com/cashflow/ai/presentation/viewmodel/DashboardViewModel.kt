package com.cashflow.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.CategoryExpense
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.MonthlyTotal
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSummary
import com.cashflow.ai.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val selectedDateRange: DateRange = DateRange.THIS_MONTH,
    val summary: TransactionSummary = TransactionSummary(),
    val categoryExpenses: List<CategoryExpense> = emptyList(),
    val monthlyTrends: List<MonthlyTotal> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedDateRange = MutableStateFlow(DateRange.THIS_MONTH)
    private val _isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = _selectedDateRange.flatMapLatest { dateRange ->
        val summaryFlow = transactionRepository.getSummary(dateRange)
        val categoryExpensesFlow = transactionRepository.getCategoryExpenses(dateRange)
        val monthlyTrendsFlow = transactionRepository.getMonthlyTrends(DateRange.LAST_6_MONTHS)
        val recentTransactionsFlow = transactionRepository.getTransactions(dateRange = dateRange)
        val categoriesFlow = transactionRepository.getAllCategories()

        combine(
            summaryFlow,
            categoryExpensesFlow,
            monthlyTrendsFlow,
            recentTransactionsFlow,
            categoriesFlow
        ) { summary, categoryExpenses, monthlyTrends, transactions, categories ->
            DashboardUiState(
                selectedDateRange = dateRange,
                summary = summary,
                categoryExpenses = categoryExpenses,
                monthlyTrends = monthlyTrends,
                recentTransactions = transactions.take(5),
                categories = categories,
                isLoading = false,
                isRefreshing = _isRefreshing.value
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun onDateRangeChanged(newRange: DateRange) {
        _selectedDateRange.value = newRange
    }

    class Factory(
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(transactionRepository) as T
        }
    }
}
