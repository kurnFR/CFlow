package com.cashflow.ai.domain.usecase.ai

import com.cashflow.ai.domain.ai.CategoryClassifier
import com.cashflow.ai.domain.model.CategorySuggestion
import com.cashflow.ai.domain.model.TransactionType

class SuggestCategoryUseCase(
    private val categoryClassifier: CategoryClassifier
) {
    suspend operator fun invoke(
        description: String,
        merchant: String? = null,
        amount: Double? = null,
        type: TransactionType = TransactionType.EXPENSE
    ): CategorySuggestion {
        return categoryClassifier.classify(
            description = description,
            merchant = merchant,
            amount = amount,
            type = type
        )
    }
}
