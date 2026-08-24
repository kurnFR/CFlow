package com.cashflow.ai.domain.model

data class MonthlyTotal(
    val month: String, // YYYY-MM
    val incomeTotal: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val netTotal: Double = incomeTotal - expenseTotal
)
