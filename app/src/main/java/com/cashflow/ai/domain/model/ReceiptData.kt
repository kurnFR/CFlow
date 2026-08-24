package com.cashflow.ai.domain.model

data class ReceiptData(
    val merchant: String? = null,
    val total: Double? = null,
    val currency: Currency = Currency.IDR,
    val date: String? = null, // YYYY-MM-DD
    val tax: Double? = null,
    val discount: Double? = null,
    val items: List<String> = emptyList(),
    val confidence: ReceiptConfidence = ReceiptConfidence(),
    val rawText: String = "",
    val suggestedCategory: String? = null,
    val categoryConfidence: Double = 0.0
)
