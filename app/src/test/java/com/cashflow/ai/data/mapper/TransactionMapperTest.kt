package com.cashflow.ai.data.mapper

import com.cashflow.ai.data.local.entity.TransactionEntity
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMapperTest {

    @Test
    fun testTransactionToEntityAndBack() {
        val domain = Transaction(
            id = 10,
            uuid = "test-uuid-123",
            date = "2026-08-20",
            description = "Lunch at Warung Padang",
            amount = 45000.0,
            category = "Food & Dining",
            type = TransactionType.EXPENSE,
            source = TransactionSource.PHOTO,
            notes = "Receipt scanned",
            currency = Currency.IDR,
            aiConfidence = 0.95,
            aiMerchant = "Warung Padang"
        )

        val entity = domain.toEntity()
        assertEquals(10L, entity.id)
        assertEquals("test-uuid-123", entity.uuid)
        assertEquals("EXPENSE", entity.type)
        assertEquals("PHOTO", entity.source)
        assertEquals("IDR", entity.currency)
        assertEquals(0.95, entity.aiConfidence)

        val backToDomain = entity.toDomain()
        assertEquals(domain.id, backToDomain.id)
        assertEquals(domain.uuid, backToDomain.uuid)
        assertEquals(domain.description, backToDomain.description)
        assertEquals(domain.type, backToDomain.type)
        assertEquals(domain.source, backToDomain.source)
        assertEquals(domain.currency, backToDomain.currency)
    }
}
