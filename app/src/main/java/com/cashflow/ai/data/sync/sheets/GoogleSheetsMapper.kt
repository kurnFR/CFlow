package com.cashflow.ai.data.sync.sheets

import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType
import java.util.UUID
import kotlin.math.abs

object GoogleSheetsMapper {

    val SHEET_HEADERS = listOf(
        "Date",           // A
        "Description",    // B
        "Amount",         // C (positive for Income, negative for Expense)
        "Category",       // D
        "Type",           // E (Income/Expense)
        "Source",         // F (Manual/Photo)
        "Image URL",      // G
        "Created At",     // H
        "Notes",          // I
        "Currency",       // J
        "AI Confidence",  // K
        "Sync Version",   // L
        "Transaction ID"  // M (UUID)
    )

    fun toSheetRow(transaction: Transaction): List<Any> {
        val signedAmount = if (transaction.type == TransactionType.EXPENSE) {
            -abs(transaction.amount)
        } else {
            abs(transaction.amount)
        }

        return listOf(
            transaction.date,                                     // A
            transaction.description,                              // B
            signedAmount,                                         // C
            transaction.category,                                 // D
            transaction.type.name,                                // E
            transaction.source.name,                              // F
            transaction.imageUrl ?: "",                           // G
            transaction.createdAt.toString(),                     // H
            transaction.notes ?: "",                              // I
            transaction.currency.name,                            // J
            transaction.aiConfidence?.toString() ?: "",           // K
            transaction.syncVersion,                              // L
            transaction.uuid                                      // M
        )
    }

    fun fromSheetRow(row: List<Any?>): Transaction? {
        if (row.size < 4) return null

        try {
            val date = row.getOrNull(0)?.toString()?.trim() ?: return null
            val description = row.getOrNull(1)?.toString()?.trim() ?: return null
            val rawAmount = row.getOrNull(2)?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: return null
            val category = row.getOrNull(3)?.toString()?.trim() ?: "Other"

            val typeStr = row.getOrNull(4)?.toString()?.trim()?.uppercase()
            val type = when {
                typeStr == "INCOME" || rawAmount > 0 -> TransactionType.INCOME
                typeStr == "EXPENSE" || rawAmount < 0 -> TransactionType.EXPENSE
                else -> TransactionType.EXPENSE
            }

            val sourceStr = row.getOrNull(5)?.toString()?.trim()?.uppercase()
            val source = if (sourceStr == "PHOTO") TransactionSource.PHOTO else TransactionSource.MANUAL

            val imageUrl = row.getOrNull(6)?.toString()?.trim().takeIf { !it.isNullOrBlank() }
            val createdAt = row.getOrNull(7)?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
            val notes = row.getOrNull(8)?.toString()?.trim().takeIf { !it.isNullOrBlank() }

            val currencyStr = row.getOrNull(9)?.toString()?.trim()?.uppercase()
            val currency = if (currencyStr == "USD") Currency.USD else Currency.IDR

            val aiConfidence = row.getOrNull(10)?.toString()?.toDoubleOrNull()
            val syncVersion = row.getOrNull(11)?.toString()?.toIntOrNull() ?: 1
            val uuid = row.getOrNull(12)?.toString()?.trim().takeIf { !it.isNullOrBlank() } ?: UUID.randomUUID().toString()

            return Transaction(
                id = 0,
                uuid = uuid,
                date = date,
                description = description,
                amount = abs(rawAmount),
                category = category,
                type = type,
                source = source,
                imageUrl = imageUrl,
                createdAt = createdAt,
                updatedAt = createdAt,
                notes = notes,
                currency = currency,
                isSynced = true,
                syncVersion = syncVersion,
                aiConfidence = aiConfidence
            )
        } catch (e: Exception) {
            return null
        }
    }
}
