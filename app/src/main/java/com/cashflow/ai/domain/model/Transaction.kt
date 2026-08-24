package com.cashflow.ai.domain.model

import java.util.UUID

data class Transaction(
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD
    val description: String,
    val amount: Double, // Always positive in domain; type indicates income/expense
    val category: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val source: TransactionSource = TransactionSource.MANUAL,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val currency: Currency = Currency.IDR,
    val isSynced: Boolean = false,
    val syncVersion: Int = 1,
    val aiConfidence: Double? = null, // 0.0 - 1.0
    val aiMerchant: String? = null,
    val tax: Double? = null,
    val discount: Double? = null,
    val itemsSummary: String? = null
)
