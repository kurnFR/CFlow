package com.cashflow.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val dateRange: DateRange = DateRange.THIS_MONTH,
    val customStartDate: String? = null,
    val customEndDate: String? = null,
    val selectedCategory: String? = null,
    val selectedType: TransactionType? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val infoMessage: String? = null
)

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _dateRange = MutableStateFlow(DateRange.THIS_MONTH)
    private val _customStartDate = MutableStateFlow<String?>(null)
    private val _customEndDate = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _searchQuery = MutableStateFlow("")

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _infoMessage = MutableStateFlow<String?>(null)

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            transactionRepository.getAllCategories().collect { list ->
                _categories.value = list
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionListUiState> = combine(
        _dateRange,
        _customStartDate,
        _customEndDate,
        _selectedCategory,
        _selectedType,
        _searchQuery
    ) { dateRange, customStart, customEnd, category, type, query ->
        TransactionFilterCriteria(dateRange, customStart, customEnd, category, type, query)
    }.flatMapLatest { criteria ->
        transactionRepository.getTransactions(
            dateRange = criteria.dateRange,
            startDate = criteria.customStartDate,
            endDate = criteria.customEndDate,
            category = criteria.category,
            type = criteria.type,
            searchQuery = criteria.query
        )
    }.combine(_categories) { transactions, categories ->
        TransactionListUiState(
            transactions = transactions,
            categories = categories,
            dateRange = _dateRange.value,
            customStartDate = _customStartDate.value,
            customEndDate = _customEndDate.value,
            selectedCategory = _selectedCategory.value,
            selectedType = _selectedType.value,
            searchQuery = _searchQuery.value,
            isLoading = _isLoading.value,
            infoMessage = _infoMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionListUiState(isLoading = true)
    )

    fun onDateRangeChanged(newRange: DateRange) {
        _dateRange.value = newRange
    }

    fun onCustomDateRangeSelected(startDate: String, endDate: String) {
        _customStartDate.value = startDate
        _customEndDate.value = endDate
        _dateRange.value = DateRange.CUSTOM
    }

    fun onCategoryFilterChanged(category: String?) {
        _selectedCategory.value = category
    }

    fun onTypeFilterChanged(type: TransactionType?) {
        _selectedType.value = type
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val result = transactionRepository.deleteTransaction(transaction)
            if (result.isSuccess) {
                _infoMessage.value = "Transaction deleted"
            }
        }
    }

    fun clearInfoMessage() {
        _infoMessage.value = null
    }

    private data class TransactionFilterCriteria(
        val dateRange: DateRange,
        val customStartDate: String?,
        val customEndDate: String?,
        val category: String?,
        val type: TransactionType?,
        val query: String
    )

    class Factory(
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionListViewModel(transactionRepository) as T
        }
    }
}
