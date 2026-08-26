package com.cashflow.ai.domain.usecase.ai

import com.cashflow.ai.data.ai.parser.NaturalLanguageTransactionParser
import com.cashflow.ai.domain.model.ParsedQuickTransaction

class ParseNaturalLanguageTransactionsUseCase(
    private val parser: NaturalLanguageTransactionParser
) {
    suspend operator fun invoke(input: String): List<ParsedQuickTransaction> {
        return parser.parse(input)
    }
}
