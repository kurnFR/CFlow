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
}
