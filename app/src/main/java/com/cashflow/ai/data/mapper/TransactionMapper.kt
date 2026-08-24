package com.cashflow.ai.data.mapper

import com.cashflow.ai.data.local.entity.TransactionEntity
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        uuid = uuid,
        date = date,
        description = description,
        amount = amount,
        category = category,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE },
        source = try { TransactionSource.valueOf(source) } catch (e: Exception) { TransactionSource.MANUAL },
        imageUrl = imageUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        notes = notes,
        currency = try { Currency.valueOf(currency) } catch (e: Exception) { Currency.IDR },
        isSynced = isSynced,
        syncVersion = syncVersion,
        aiConfidence = aiConfidence,
        aiMerchant = aiMerchant,
        tax = tax,
        discount = discount,
        itemsSummary = itemsSummary
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        uuid = uuid,
        date = date,
        description = description,
        amount = amount,
        category = category,
        type = type.name,
        source = source.name,
        imageUrl = imageUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        notes = notes,
        currency = currency.name,
        isSynced = isSynced,
        syncVersion = syncVersion,
        aiConfidence = aiConfidence,
        aiMerchant = aiMerchant,
        tax = tax,
        discount = discount,
        itemsSummary = itemsSummary
    )
}
