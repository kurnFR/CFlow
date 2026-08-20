package com.cashflow.ai.domain.model

data class ReceiptItem(
    val name: String,
    val price: Double? = null,
    val quantity: Int? = 1
)
