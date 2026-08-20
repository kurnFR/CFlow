package com.cashflow.ai.domain.ai

import com.cashflow.ai.domain.model.CategorySuggestion
import com.cashflow.ai.domain.model.TransactionType

interface CategoryClassifier {
    suspend fun classify(
        description: String,
        merchant: String? = null,
        amount: Double? = null,
        type: TransactionType = TransactionType.EXPENSE
    ): CategorySuggestion
}
