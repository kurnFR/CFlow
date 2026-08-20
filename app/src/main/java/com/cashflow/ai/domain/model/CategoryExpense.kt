package com.cashflow.ai.domain.model

data class CategoryExpense(
    val category: String,
    val total: Double,
    val percentage: Float = 0f,
    val icon: String = "🏷️",
    val colorHex: String = "#006A6A"
)
