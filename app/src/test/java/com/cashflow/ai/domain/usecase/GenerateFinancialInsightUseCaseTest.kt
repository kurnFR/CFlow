package com.cashflow.ai.domain.usecase

import com.cashflow.ai.domain.model.CategoryExpense
import com.cashflow.ai.domain.model.TransactionSummary
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateFinancialInsightUseCaseTest {
    @Test
    fun negativeCashFlowSuggestsSpendingReset() {
        val insight = GenerateFinancialInsightUseCase()(
            period = "THIS_MONTH",
            summary = TransactionSummary(totalIncome = 100.0, totalExpense = 140.0, netBalance = -40.0),
            categoryExpenses = listOf(CategoryExpense("Dining", 70.0, 50f))
        )

        assertTrue(insight.headline.contains("reset"))
        assertTrue(insight.body.contains("Dining"))
    }

    @Test
    fun positiveCashFlowSuggestsReducingLargestExpense() {
        val insight = GenerateFinancialInsightUseCase()(
            period = "THIS_MONTH",
            summary = TransactionSummary(totalIncome = 20000.0, totalExpense = 15000.0, netBalance = 5000.0),
            categoryExpenses = listOf(
                CategoryExpense("Food & Dining", 5200.0, 34.7f),
                CategoryExpense("Transport", 2800.0, 18.7f),
                CategoryExpense("Shopping", 2100.0, 14.0f)
            )
        )

        assertTrue(insight.headline.contains("positive") || insight.headline.contains("cash flow") || insight.headline.contains("surplus"))
        assertTrue(insight.body.contains("Food & Dining") || insight.body.contains("reduce"))
        assertTrue(insight.recommendations.size >= 2)
    }

    @Test
    fun allHistoryPeriodIncludesHistorySpecificGuidance() {
        val insight = GenerateFinancialInsightUseCase()(
            period = "ALL_HISTORY",
            summary = TransactionSummary(totalIncome = 120000.0, totalExpense = 90000.0, netBalance = 30000.0),
            categoryExpenses = listOf(
                CategoryExpense("Housing", 32000.0, 35.6f),
                CategoryExpense("Food", 18000.0, 20.0f)
            )
        )

        assertTrue(insight.body.contains("all history") || insight.body.contains("long term") || insight.body.contains("history"))
    }
}
