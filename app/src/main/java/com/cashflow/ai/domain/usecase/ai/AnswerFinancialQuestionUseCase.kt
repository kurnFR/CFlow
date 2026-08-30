package com.cashflow.ai.domain.usecase.ai

import com.cashflow.ai.data.ai.analytics.FinancialAnalyticsEngine
import com.cashflow.ai.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Answers financial questions using the user's own transaction data.
 * Deterministic, data-grounded: computes numbers locally; optionally re-phrases with Gemini.
 */
class AnswerFinancialQuestionUseCase(
    private val analyticsEngine: FinancialAnalyticsEngine
) {
    suspend operator fun invoke(
        query: String,
        transactions: List<Transaction>,
        currency: com.cashflow.ai.domain.model.Currency
    ): FinancialAnalyticsEngine.QueryResult = withContext(Dispatchers.Default) {
        analyticsEngine.answer(query, transactions, currency)
    }
}