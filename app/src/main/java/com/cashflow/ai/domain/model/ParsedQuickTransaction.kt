package com.cashflow.ai.domain.model

data class ParsedQuickTransaction(
    val description: String,
    val amount: Double,
    val category: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val date: String,
    val currency: Currency = Currency.IDR,
    val confidence: Double = 0.90,
    val rawSnippet: String = ""
) {
    fun toTransaction(): Transaction {
        return Transaction(
            id = 0,
            date = date,
            description = description,
            amount = amount,
            category = category,
            type = type,
            source = TransactionSource.MANUAL,
            currency = currency,
            aiConfidence = confidence,
            aiMerchant = description
        )
    }
}
