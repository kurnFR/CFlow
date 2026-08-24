package com.cashflow.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.ReceiptData
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.repository.TransactionRepository
import com.cashflow.ai.domain.usecase.ai.SuggestCategoryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class AddEditTransactionUiState(
    val transactionId: Long? = null,
    val transactionUuid: String? = null,
    val isEditMode: Boolean = false,
    val description: String = "",
    val amountText: String = "",
    val currency: Currency = Currency.IDR,
    val category: String = "Food & Dining",
    val type: TransactionType = TransactionType.EXPENSE,
    val date: String = DateUtils.getCurrentDateString(),
    val taxText: String = "",
    val discountText: String = "",
    val notes: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val imageUrl: String? = null,
    val aiConfidence: Double? = null,
    val aiMerchant: String? = null,
    val itemsSummary: String? = null,
    val suggestedCategory: String? = null,
    val categories: List<Category> = emptyList(),
    val isUserSelectedCategory: Boolean = false,
    val isSaving: Boolean = false,
    val isSavedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)

class AddEditTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val suggestCategoryUseCase: SuggestCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    private var suggestJob: Job? = null

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            transactionRepository.getAllCategories().collect { list ->
                _uiState.update { state ->
                    state.copy(
                        categories = list,
                        category = if (state.category.isBlank() && list.isNotEmpty()) list.first().name else state.category
                    )
                }
            }
        }
    }

    fun onDescriptionChanged(newDescription: String) {
        _uiState.update { it.copy(description = newDescription, errorMessage = null) }

        // Trigger AI category suggest
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(300) // Debounce
            if (newDescription.length >= 2) {
                val suggestion = suggestCategoryUseCase(
                    description = newDescription,
                    type = _uiState.value.type
                )
                _uiState.update { state ->
                    state.copy(
                        suggestedCategory = suggestion.category,
                        category = if (!state.isUserSelectedCategory) suggestion.category else state.category
                    )
                }
            }
        }
    }

    fun onAmountChanged(newAmount: String) {
        _uiState.update { it.copy(amountText = newAmount, errorMessage = null) }
    }

    fun onCurrencyChanged(newCurrency: Currency) {
        _uiState.update { it.copy(currency = newCurrency) }
    }

    fun onCategoryChanged(newCategory: String) {
        _uiState.update { it.copy(category = newCategory, isUserSelectedCategory = true) }
    }

    fun onTypeChanged(newType: TransactionType) {
        _uiState.update { it.copy(type = newType) }
        // Re-suggest category based on type
        if (_uiState.value.description.length >= 2) {
            viewModelScope.launch {
                val suggestion = suggestCategoryUseCase(
                    description = _uiState.value.description,
                    type = newType
                )
                _uiState.update { state ->
                    state.copy(
                        suggestedCategory = suggestion.category,
                        category = if (!state.isUserSelectedCategory) suggestion.category else state.category
                    )
                }
            }
        }
    }

    fun onDateChanged(newDate: String) {
        _uiState.update { it.copy(date = newDate) }
    }

    fun onTaxChanged(newTax: String) {
        _uiState.update { it.copy(taxText = newTax) }
    }

    fun onDiscountChanged(newDiscount: String) {
        _uiState.update { it.copy(discountText = newDiscount) }
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    fun populateFromReceiptJson(receiptJson: String) {
        try {
            val json = JSONObject(receiptJson)
            val merchant = if (json.has("merchant") && !json.isNull("merchant")) json.getString("merchant") else ""
            val total = if (json.has("total") && !json.isNull("total")) json.getDouble("total") else 0.0
            val currencyStr = if (json.has("currency") && !json.isNull("currency")) json.getString("currency") else "IDR"
            val dateStr = if (json.has("date") && !json.isNull("date")) json.getString("date") else DateUtils.getCurrentDateString()
            val category = if (json.has("suggestedCategory") && !json.isNull("suggestedCategory")) json.getString("suggestedCategory") else "Food & Dining"
            val tax = if (json.has("tax") && !json.isNull("tax")) json.getDouble("tax") else null
            val discount = if (json.has("discount") && !json.isNull("discount")) json.getDouble("discount") else null
            val confidence = if (json.has("confidence") && !json.isNull("confidence")) {
                val c = json.getJSONObject("confidence")
                if (c.has("overall")) c.getDouble("overall") else 0.90
            } else 0.90

            val itemsList = mutableListOf<String>()
            if (json.has("items") && !json.isNull("items")) {
                val arr = json.getJSONArray("items")
                for (i in 0 until arr.length()) {
                    itemsList.add(arr.getString(i))
                }
            }

            _uiState.update { state ->
                state.copy(
                    description = merchant.ifBlank { itemsList.firstOrNull() ?: "Receipt Purchase" },
                    amountText = if (total > 0.0) {
                        if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                    } else "",
                    currency = if (currencyStr.equals("USD", ignoreCase = true)) Currency.USD else Currency.IDR,
                    category = category,
                    suggestedCategory = category,
                    type = TransactionType.EXPENSE,
                    date = dateStr,
                    taxText = tax?.toString() ?: "",
                    discountText = discount?.toString() ?: "",
                    source = TransactionSource.PHOTO,
                    aiConfidence = confidence,
                    aiMerchant = merchant,
                    itemsSummary = if (itemsList.isNotEmpty()) itemsList.take(3).joinToString("; ") else null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadTransactionForEdit(id: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(id).firstOrNull() ?: return@launch
            _uiState.update { state ->
                state.copy(
                    transactionId = transaction.id,
                    transactionUuid = transaction.uuid,
                    isEditMode = true,
                    description = transaction.description,
                    amountText = if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString() else transaction.amount.toString(),
                    currency = transaction.currency,
                    category = transaction.category,
                    type = transaction.type,
                    date = transaction.date,
                    taxText = transaction.tax?.toString() ?: "",
                    discountText = transaction.discount?.toString() ?: "",
                    notes = transaction.notes ?: "",
                    source = transaction.source,
                    imageUrl = transaction.imageUrl,
                    aiConfidence = transaction.aiConfidence,
                    aiMerchant = transaction.aiMerchant,
                    itemsSummary = transaction.itemsSummary
                )
            }
        }
    }

    fun saveTransaction() {
        val state = _uiState.value

        val desc = state.description.trim()
        if (desc.length < 2) {
            _uiState.update { it.copy(errorMessage = "Description must be at least 2 characters") }
            return
        }

        val parsedAmount = state.amountText.replace(",", ".").toDoubleOrNull()
        if (parsedAmount == null || !parsedAmount.isFinite() || parsedAmount <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid positive amount") }
            return
        }

        if (!DateUtils.isValidDate(state.date)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid date") }
            return
        }

        val parsedTax = state.taxText.replace(",", ".").toDoubleOrNull()
        val parsedDiscount = state.discountText.replace(",", ".").toDoubleOrNull()

        if (parsedTax != null && (!parsedTax.isFinite() || parsedTax < 0.0) ||
            parsedDiscount != null && (!parsedDiscount.isFinite() || parsedDiscount < 0.0)
        ) {
            _uiState.update { it.copy(errorMessage = "Tax and discount must be valid non-negative amounts") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val transaction = Transaction(
                id = state.transactionId ?: 0,
                uuid = state.transactionUuid ?: UUID.randomUUID().toString(),
                date = state.date,
                description = desc,
                amount = parsedAmount,
                category = state.category,
                type = state.type,
                source = state.source,
                imageUrl = state.imageUrl,
                notes = state.notes.ifBlank { null },
                currency = state.currency,
                isSynced = false,
                syncVersion = 1,
                aiConfidence = state.aiConfidence,
                aiMerchant = state.aiMerchant,
                tax = parsedTax,
                discount = parsedDiscount,
                itemsSummary = state.itemsSummary
            )

            val result = if (state.isEditMode) {
                transactionRepository.updateTransaction(transaction)
            } else {
                transactionRepository.insertTransaction(transaction)
            }

            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isSavedSuccessfully = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to save transaction"
                    )
                }
            }
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val suggestCategoryUseCase: SuggestCategoryUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditTransactionViewModel(transactionRepository, suggestCategoryUseCase) as T
        }
    }
}
