package com.cashflow.ai.presentation.viewmodel

import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.repository.TransactionRepository
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
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: TransactionRepository
    private lateinit var viewModel: TransactionListViewModel

    private val sampleTransactions = listOf(
        Transaction(id = 1, description = "Lunch", amount = 45000.0, category = "Food & Dining", date = "2026-08-20"),
        Transaction(id = 2, description = "Taxi", amount = 30000.0, category = "Transport", date = "2026-08-19")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock(TransactionRepository::class.java)

        `when`(mockRepository.getAllCategories()).thenReturn(flowOf(emptyList()))
        `when`(mockRepository.getTransactions(
            dateRange = org.mockito.ArgumentMatchers.any() ?: DateRange.THIS_MONTH,
            category = org.mockito.ArgumentMatchers.any(),
            type = org.mockito.ArgumentMatchers.any(),
            searchQuery = org.mockito.ArgumentMatchers.any()
        )).thenReturn(flowOf(sampleTransactions))

        viewModel = TransactionListViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onDateRangeChanged_updatesDateRange() = runTest {
        viewModel.onDateRangeChanged(DateRange.TODAY)
        advanceUntilIdle()

        assertEquals(DateRange.TODAY, viewModel.uiState.value.dateRange)
    }

    @Test
    fun onSearchQueryChanged_updatesSearchQuery() = runTest {
        viewModel.onSearchQueryChanged("Starbucks")
        advanceUntilIdle()

        assertEquals("Starbucks", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun deleteTransaction_callsRepositoryAndSetsMessage() = runTest {
        val target = sampleTransactions.first()
        `when`(mockRepository.deleteTransaction(target)).thenReturn(Result.success(Unit))

        viewModel.deleteTransaction(target)
        advanceUntilIdle()

        assertEquals("Transaction deleted", viewModel.uiState.value.infoMessage)
    }
}
