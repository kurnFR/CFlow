package com.cashflow.ai.data.sync.sheets

import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSheetsMapperTest {

    @Test
    fun toSheetRow_expenseTransaction_createsCorrect13ColumnsWithNegativeAmount() {
        val transaction = Transaction(
            id = 1,
            uuid = "test-uuid-1234",
            date = "2026-08-21",
            description = "Starbucks Coffee",
            amount = 65000.0,
            category = "Food & Dining",
            type = TransactionType.EXPENSE,
            source = TransactionSource.PHOTO,
            imageUrl = "https://drive.google.com/file/d/xyz",
            createdAt = 1787300000000L,
            notes = "With client",
            currency = Currency.IDR,
            syncVersion = 1,
            aiConfidence = 0.95
        )

        val row = GoogleSheetsMapper.toSheetRow(transaction)

        assertEquals(13, row.size)
        assertEquals("2026-08-21", row[0])              // A: Date
        assertEquals("Starbucks Coffee", row[1])         // B: Description
        assertEquals(-65000.0, row[2])                   // C: Signed Amount (- for expense)
        assertEquals("Food & Dining", row[3])            // D: Category
        assertEquals("EXPENSE", row[4])                  // E: Type
        assertEquals("PHOTO", row[5])                    // F: Source
        assertEquals("https://drive.google.com/file/d/xyz", row[6]) // G: Image URL
        assertEquals("1787300000000", row[7])            // H: Created At
        assertEquals("With client", row[8])              // I: Notes
        assertEquals("IDR", row[9])                      // J: Currency
        assertEquals("0.95", row[10])                    // K: AI Confidence
        assertEquals(1, row[11])                         // L: Sync Version
        assertEquals("test-uuid-1234", row[12])          // M: Transaction ID
    }

    @Test
    fun toSheetRow_incomeTransaction_createsPositiveAmount() {
        val transaction = Transaction(
            id = 2,
            uuid = "uuid-income-999",
            date = "2026-08-21",
            description = "Monthly Salary",
            amount = 15000000.0,
            category = "Salary",
            type = TransactionType.INCOME,
            source = TransactionSource.MANUAL,
            currency = Currency.IDR
        )

        val row = GoogleSheetsMapper.toSheetRow(transaction)
        assertEquals(15000000.0, row[2]) // C: Positive amount for income
        assertEquals("INCOME", row[4])
        assertEquals("MANUAL", row[5])
    }

    @Test
    fun fromSheetRow_validSheetRow_parsesCorrectly() {
        val row = listOf(
            "2026-08-20",                                 // A: Date
            "Indomaret Groceries",                        // B: Description
            "-45000",                                     // C: Amount
            "Groceries",                                  // D: Category
            "EXPENSE",                                    // E: Type
            "PHOTO",                                      // F: Source
            "https://drive.google.com/xyz",               // G: Image URL
            "1787200000000",                              // H: Created At
            "Weekly supplies",                            // I: Notes
            "IDR",                                        // J: Currency
            "0.92",                                       // K: AI Confidence
            "1",                                          // L: Sync Version
            "uuid-remote-555"                             // M: UUID
        )

        val transaction = GoogleSheetsMapper.fromSheetRow(row)
        assertNotNull(transaction)
        assertEquals("uuid-remote-555", transaction!!.uuid)
        assertEquals("2026-08-20", transaction.date)
        assertEquals("Indomaret Groceries", transaction.description)
        assertEquals(45000.0, transaction.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals("Groceries", transaction.category)
        assertEquals(TransactionSource.PHOTO, transaction.source)
        assertEquals("https://drive.google.com/xyz", transaction.imageUrl)
        assertEquals("Weekly supplies", transaction.notes)
        assertEquals(Currency.IDR, transaction.currency)
        assertEquals(0.92, transaction.aiConfidence ?: 0.0, 0.01)
        assertTrue(transaction.isSynced)
    }
}
