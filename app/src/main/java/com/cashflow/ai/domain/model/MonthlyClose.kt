package com.cashflow.ai.domain.model

/** Latest persisted financial snapshot for a calendar month. */
data class MonthlyClose(
    val month: String,
    val closedAt: Long? = null,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0,
    val topExpenseCategory: String? = null,
    val insight: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = false
)
