package com.cashflow.ai.presentation.viewmodel

import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.CategorySource
import com.cashflow.ai.domain.model.CategorySuggestion
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.repository.TransactionRepository
import com.cashflow.ai.domain.usecase.ai.SuggestCategoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: TransactionRepository
    private lateinit var mockSuggestCategoryUseCase: SuggestCategoryUseCase
    private lateinit var viewModel: AddEditTransactionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock(TransactionRepository::class.java)
        mockSuggestCategoryUseCase = mock(SuggestCategoryUseCase::class.java)

        val sampleCategories = listOf(
            Category(id = 1, name = "Food & Dining", icon = "🍔"),
            Category(id = 2, name = "Groceries", icon = "🛒"),
            Category(id = 3, name = "Salary", icon = "💼", type = TransactionType.INCOME)
        )
        `when`(mockRepository.getAllCategories()).thenReturn(flowOf(sampleCategories))

        viewModel = AddEditTransactionViewModel(mockRepository, mockSuggestCategoryUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onDescriptionChanged_triggersCategorySuggestion() = runTest {
        val suggestion = CategorySuggestion(
            category = "Groceries",
            confidence = 0.95,
            source = CategorySource.RULE
        )
        `when`(mockSuggestCategoryUseCase.invoke(
            description = "Indomaret groceries",
            merchant = null,
            amount = null,
            type = TransactionType.EXPENSE
        )).thenReturn(suggestion)

        viewModel.onDescriptionChanged("Indomaret groceries")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Indomaret groceries", state.description)
        assertEquals("Groceries", state.suggestedCategory)
        assertEquals("Groceries", state.category)
    }

    @Test
    fun saveTransaction_validationFailsOnEmptyAmount() = runTest {
        viewModel.onDescriptionChanged("Lunch at Solaria")
        viewModel.onAmountChanged("")

        viewModel.saveTransaction()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertEquals(false, state.isSavedSuccessfully)
    }

    @Test
    fun saveTransaction_success() = runTest {
        `when`(mockRepository.insertTransaction(any())).thenReturn(Result.success(101L))

        viewModel.onDescriptionChanged("Lunch at Solaria")
        viewModel.onAmountChanged("55000")
        viewModel.onCategoryChanged("Food & Dining")

        viewModel.saveTransaction()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.errorMessage)
        assertEquals(true, state.isSavedSuccessfully)
    }

    @Test
    fun populateFromReceiptJson_populatesAllFields() = runTest {
        val receiptJson = """
            {
              "merchant": "Starbucks Coffee",
              "total": 65000.0,
              "currency": "IDR",
              "date": "2026-08-20",
              "suggestedCategory": "Food & Dining",
              "tax": 6500.0,
              "discount": 5000.0,
              "items": ["Caramel Macchiato"],
              "confidence": { "overall": 0.95 }
            }
        """.trimIndent()

        viewModel.populateFromReceiptJson(receiptJson)

        val state = viewModel.uiState.value
        assertEquals("Starbucks Coffee", state.description)
        assertEquals("65000", state.amountText)
        assertEquals(Currency.IDR, state.currency)
        assertEquals("Food & Dining", state.category)
        assertEquals("2026-08-20", state.date)
        assertEquals("6500.0", state.taxText)
        assertEquals("5000.0", state.discountText)
        assertEquals(TransactionSource.PHOTO, state.source)
        assertEquals(0.95, state.aiConfidence ?: 0.0, 0.01)
    }
}
