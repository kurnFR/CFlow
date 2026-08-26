package com.cashflow.ai.presentation.viewmodel

import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.CategoryExpense
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.MonthlyTotal
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSummary
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: TransactionRepository
    private lateinit var viewModel: DashboardViewModel

    private val sampleSummary = TransactionSummary(
        totalIncome = 10000000.0,
        totalExpense = 4500000.0,
        netBalance = 5500000.0,
        incomeGrowthPercent = 12f,
        expenseGrowthPercent = -5f,
        currency = Currency.IDR
    )

    private val sampleCategoryExpenses = listOf(
        CategoryExpense(category = "Food & Dining", total = 2500000.0, percentage = 55.5f, icon = "🍔"),
        CategoryExpense(category = "Transport", total = 2000000.0, percentage = 44.5f, icon = "🚗")
    )

    private val sampleMonthlyTrends = listOf(
        MonthlyTotal(month = "2026-06", incomeTotal = 8000000.0, expenseTotal = 4000000.0),
        MonthlyTotal(month = "2026-07", incomeTotal = 9000000.0, expenseTotal = 4200000.0),
        MonthlyTotal(month = "2026-08", incomeTotal = 10000000.0, expenseTotal = 4500000.0)
    )

    private val sampleTransactions = (1..10).map { id ->
        Transaction(
            id = id.toLong(),
            description = "Expense $id",
            amount = 50000.0 * id,
            category = "Food & Dining",
            date = "2026-08-20"
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock(TransactionRepository::class.java)

        `when`(mockRepository.getSummary(org.mockito.ArgumentMatchers.any() ?: DateRange.THIS_MONTH))
            .thenReturn(flowOf(sampleSummary))
        `when`(mockRepository.getCategoryExpenses(org.mockito.ArgumentMatchers.any() ?: DateRange.THIS_MONTH))
            .thenReturn(flowOf(sampleCategoryExpenses))
        `when`(mockRepository.getMonthlyTrends(org.mockito.ArgumentMatchers.any() ?: DateRange.LAST_6_MONTHS))
            .thenReturn(flowOf(sampleMonthlyTrends))
        `when`(mockRepository.getTransactions(
            dateRange = org.mockito.ArgumentMatchers.any() ?: DateRange.THIS_MONTH,
            category = org.mockito.ArgumentMatchers.any(),
            type = org.mockito.ArgumentMatchers.any(),
            searchQuery = org.mockito.ArgumentMatchers.any()
        )).thenReturn(flowOf(sampleTransactions))
        `when`(mockRepository.getAllCategories()).thenReturn(flowOf(emptyList()))
        `when`(mockRepository.getLatestMonthlyClose()).thenReturn(flowOf(null))

        viewModel = DashboardViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initializesWithSummaryAndAggregations() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(10000000.0, state.summary.totalIncome, 0.01)
        assertEquals(4500000.0, state.summary.totalExpense, 0.01)
        assertEquals(5500000.0, state.summary.netBalance, 0.01)
        assertEquals(2, state.categoryExpenses.size)
        assertEquals(3, state.monthlyTrends.size)
        // Limits recent transactions to top 5
        assertEquals(5, state.recentTransactions.size)
    }

    @Test
    fun onDateRangeChanged_updatesSelectedRange() = runTest {
        viewModel.onDateRangeChanged(DateRange.THIS_WEEK)
        advanceUntilIdle()

        assertEquals(DateRange.THIS_WEEK, viewModel.uiState.value.selectedDateRange)
    }

    @Test
    fun onChatInputChanged_updatesChatText() = runTest {
        viewModel.onChatInputChanged("bensin 30k")
        advanceUntilIdle()

        assertEquals("bensin 30k", viewModel.uiState.value.chatInputText)
    }
}
