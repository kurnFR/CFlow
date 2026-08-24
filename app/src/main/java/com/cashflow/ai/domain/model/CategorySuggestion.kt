package com.cashflow.ai.domain.model

enum class CategorySource {
    RULE,
    HISTORY,
    AI,
    DEFAULT
}

data class CategorySuggestion(
    val category: String,
    val confidence: Double = 1.0,
    val source: CategorySource = CategorySource.RULE
)
