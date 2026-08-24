package com.cashflow.ai.domain.model

data class TransactionSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = totalIncome - totalExpense,
    val incomeGrowthPercent: Float? = null,
    val expenseGrowthPercent: Float? = null,
    val netGrowthPercent: Float? = null,
    val currency: Currency = Currency.IDR
)
