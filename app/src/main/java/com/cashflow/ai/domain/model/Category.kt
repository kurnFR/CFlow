package com.cashflow.ai.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String = "🏷️",
    val colorHex: String = "#006A6A",
    val isDefault: Boolean = true,
    val type: TransactionType = TransactionType.EXPENSE
)
