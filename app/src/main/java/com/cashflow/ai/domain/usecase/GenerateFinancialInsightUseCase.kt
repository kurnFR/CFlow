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
        val netBalance = summary.netBalance
        val totalIncome = summary.totalIncome
        val totalExpense = summary.totalExpense
        val savingsRate = if (totalIncome > 0.0) (netBalance / totalIncome) * 100.0 else 0.0
        val topCategory = categoryExpenses.maxByOrNull { it.total }
        val largestExpense = categoryExpenses.maxByOrNull { it.total }
        val expenseShare = if (totalIncome > 0.0) (totalExpense / totalIncome) * 100.0 else 0.0

        val rangeLabel = when (period.uppercase(Locale.US)) {
            "TODAY" -> "today"
            "THIS_WEEK" -> "this week"
            "THIS_MONTH" -> "this month"
            "MONTH_TO_DATE" -> "month to date"
            "LAST_MONTH" -> "last month"
            "LAST_3_MONTHS" -> "the last 3 months"
            "LAST_6_MONTHS" -> "the last 6 months"
            "ALL_TIME", "ALL_HISTORY" -> "all history"
            "CUSTOM" -> "your selected date range"
            else -> period.lowercase(Locale.US).replace('_', ' ')
        }

        val recommendations = mutableListOf<String>()

        if (totalIncome <= 0.0) {
            recommendations += "Add income entries so the cash flow view for $rangeLabel is accurate and actionable."
        } else if (netBalance < 0.0) {
            recommendations += "Cash flow is negative for $rangeLabel. Cut non-essential spending this week and reduce impulse purchases before they become recurring."
            if (largestExpense != null) {
                val savingsTarget = largestExpense.total * 0.12
                recommendations += "Reduce ${largestExpense.category} by about ${formatCurrency(savingsTarget)} to improve the cash position for $rangeLabel quickly."
            }
        } else if (savingsRate < 10.0) {
            recommendations += "Your surplus for $rangeLabel is only ${formatPercent(savingsRate)} of income. Keep a tighter cap on lifestyle spending."
        } else {
            recommendations += "Your cash flow for $rangeLabel is positive and you retained ${formatPercent(savingsRate)} of income. Keep automating the surplus into savings or debt payoff."
        }

        if (topCategory != null && topCategory.percentage >= 25f) {
            recommendations += "${topCategory.category} is your largest expense at ${topCategory.percentage.toInt()}% of spending for $rangeLabel; review subscriptions, food delivery, and recurring costs there first."
        }

        if (expenseShare > 85.0 && totalIncome > 0.0) {
            recommendations += "Spending is still high relative to income for $rangeLabel. Aim to keep total expenses below ${formatPercent(80.0)} of income to create a safer buffer."
        }

        if (period.uppercase(Locale.US) == "ALL_TIME" || period.uppercase(Locale.US) == "ALL_HISTORY") {
            recommendations += "Look at the full history to spot recurring seasonal spikes and make long-term cuts instead of reacting to one-off month swings."
        }

        if (recommendations.size < 3) {
            recommendations += "Review your top categories weekly and record transactions close to the time they happen to keep cash flow stable across $rangeLabel."
        }

        val headline = when {
            totalIncome <= 0.0 -> "Add income to unlock a clearer plan"
            netBalance < 0.0 -> "This period needs a spending reset"
            savingsRate >= 20.0 -> "Strong positive cash flow"
            expenseShare > 85.0 -> "Cash flow is positive but tight"
            else -> "Your cash flow is moving forward"
        }

        return FinancialInsight(period, headline, recommendations.distinct())
    }

    private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)

    private fun formatCurrency(value: Double): String {
        return if (value >= 1000.0) {
            String.format(Locale.US, "Rp%,.0f", value)
        } else {
            String.format(Locale.US, "Rp%.0f", value)
        }
    }
}
