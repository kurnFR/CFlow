package com.cashflow.ai.domain.usecase

import com.cashflow.ai.domain.model.CategoryExpense
import com.cashflow.ai.domain.model.FinancialInsight
import com.cashflow.ai.domain.model.TransactionSummary
import java.util.Locale

class GenerateFinancialInsightUseCase {
    operator fun invoke(
        period: String,
        summary: TransactionSummary,
        categoryExpenses: List<CategoryExpense>
    ): FinancialInsight {
        val savingsRate = if (summary.totalIncome > 0.0) {
            (summary.netBalance / summary.totalIncome) * 100.0
        } else 0.0
        val topCategory = categoryExpenses.firstOrNull()
        val recommendations = mutableListOf<String>()

        if (summary.totalIncome <= 0.0) {
            recommendations += "Add income entries so your cash-flow picture is complete."
        } else if (summary.netBalance < 0.0) {
            recommendations += "Spending is higher than income; pause non-essential purchases and set a weekly limit."
        } else if (savingsRate < 10.0) {
            recommendations += "Your surplus is ${formatPercent(savingsRate)}; try directing at least 10% of income to savings."
        } else {
            recommendations += "You retained ${formatPercent(savingsRate)} of income; keep this habit and automate the surplus."
        }

        if (topCategory != null && topCategory.percentage >= 30f) {
            recommendations += "${topCategory.category} is your largest expense at ${topCategory.percentage.toInt()}%; review recurring or avoidable spending there."
        }
        if (recommendations.size < 2) {
            recommendations += "Review your top categories weekly and record transactions close to the time they happen."
        }

        val headline = when {
            summary.totalIncome <= 0.0 -> "Add income to unlock a clearer plan"
            summary.netBalance < 0.0 -> "This period needs a spending reset"
            savingsRate >= 20.0 -> "Strong progress this period"
            else -> "Your cash flow is moving forward"
        }
        return FinancialInsight(period, headline, recommendations)
    }

    private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)
}
