package com.cashflow.ai.data.ai.parser

import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.data.ai.category.SmartCategoryClassifier
import com.cashflow.ai.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NaturalLanguageTransactionParserTest {

    private lateinit var classifier: SmartCategoryClassifier
    private lateinit var parser: NaturalLanguageTransactionParser

    @Before
    fun setUp() {
        classifier = SmartCategoryClassifier(apiKey = "")
        parser = NaturalLanguageTransactionParser(
            categoryClassifier = classifier,
            apiKey = ""
        )
    }

    @Test
    fun testParseMultipleCommaSeparatedTransactions() = runTest {
        val input = "bensin 30000, makan nasi goreng 50.000, parkir 5000"
        val results = parser.parseDeterministic(input)

        assertEquals(3, results.size)

        // Item 1
        assertEquals("Bensin", results[0].description)
        assertEquals(30000.0, results[0].amount, 0.01)
        assertEquals("Transport", results[0].category)
        assertEquals(TransactionType.EXPENSE, results[0].type)
        assertEquals(DateUtils.today(), results[0].date)

        // Item 2
        assertTrue(results[1].description.contains("Nasi Goreng", ignoreCase = true))
        assertEquals(50000.0, results[1].amount, 0.01)
        assertEquals("Food & Dining", results[1].category)
        assertEquals(TransactionType.EXPENSE, results[1].type)

        // Item 3
        assertEquals("Parkir", results[1 + 1].description)
        assertEquals(5000.0, results[2].amount, 0.01)
        assertEquals("Transport", results[2].category)
        assertEquals(TransactionType.EXPENSE, results[2].type)
    }

    @Test
    fun testParseShorthandMultipliers() = runTest {
        val input = "bensin 30rb, makan siang 45k, parkir 5k"
        val results = parser.parseDeterministic(input)

        assertEquals(3, results.size)
        assertEquals(30000.0, results[0].amount, 0.01)
        assertEquals(45000.0, results[1].amount, 0.01)
        assertEquals(5000.0, results[2].amount, 0.01)
    }

    @Test
    fun testParseIncomeVsExpense() = runTest {
        val input = "gaji 15jt, pulsa 100k"
        val results = parser.parseDeterministic(input)

        assertEquals(2, results.size)

        // Gaji
        assertEquals(15000000.0, results[0].amount, 0.01)
        assertEquals("Salary", results[0].category)
        assertEquals(TransactionType.INCOME, results[0].type)

        // Pulsa
        assertEquals(100000.0, results[1].amount, 0.01)
        assertEquals("Bills & Utilities", results[1].category)
        assertEquals(TransactionType.EXPENSE, results[1].type)
    }

    @Test
    fun testParseYesterdayKeyword() = runTest {
        val input = "kemarin beli obat 40rb, kopi 25k"
        val results = parser.parseDeterministic(input)

        assertEquals(2, results.size)
        assertEquals(DateUtils.getYesterdayDateString(), results[0].date)
        assertEquals("Healthcare", results[0].category)
        assertEquals(40000.0, results[0].amount, 0.01)

        assertEquals(DateUtils.today(), results[1].date)
        assertEquals("Food & Dining", results[1].category)
        assertEquals(25000.0, results[1].amount, 0.01)
    }
}
