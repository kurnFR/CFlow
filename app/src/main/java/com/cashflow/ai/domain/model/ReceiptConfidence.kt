package com.cashflow.ai.domain.model

data class ReceiptConfidence(
    val merchant: Double = 0.0,
    val total: Double = 0.0,
    val date: Double = 0.0,
    val overall: Double = 0.0
) {
    val isHighConfidence: Boolean get() = overall >= 0.85
    val isMediumConfidence: Boolean get() = overall in 0.70..<0.85
    val isLowConfidence: Boolean get() = overall < 0.70
}
