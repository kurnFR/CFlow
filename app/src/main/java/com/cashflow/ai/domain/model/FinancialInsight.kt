package com.cashflow.ai.domain.model

data class FinancialInsight(
    val period: String,
    val headline: String,
    val recommendations: List<String>,
    val generatedAt: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = false
) {
    val body: String
        get() = recommendations.joinToString(" ")
}
