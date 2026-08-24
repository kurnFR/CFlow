package com.cashflow.ai.data.ai.parser

import com.cashflow.ai.domain.model.Currency
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiReceiptParserTest {

    private lateinit var parser: GeminiReceiptParser

    @Before
    fun setUp() {
        parser = GeminiReceiptParser(apiKey = "")
    }

    @Test
    fun cleanJsonMarkdown_markdownCodeBlock_cleansSuccessfully() {
        val markdownJson = """
            ```json
            {
              "merchant": "McDonald's",
              "total": 55000.0,
              "currency": "IDR",
              "date": "2026-08-19"
            }
            ```
        """.trimIndent()

        val cleaned = parser.cleanJsonMarkdown(markdownJson)
        assertTrue(cleaned.startsWith("{"))
        assertTrue(cleaned.endsWith("}"))
    }

    @Test
    fun parseJsonToReceiptData_validJson_parsesFieldsCorrectly() {
        val json = """
            {
              "merchant": "Alfamart Diponegoro",
              "total": 65000.0,
              "currency": "IDR",
              "date": "2026-08-20",
              "tax": 6500.0,
              "discount": 5000.0,
              "items": ["Kopi Kenangan", "Chitato 68g", "Air Mineral"],
              "confidence": {
                "merchant": 0.95,
                "total": 0.98,
                "date": 0.94
              }
            }
        """.trimIndent()

        val data = parser.parseJsonToReceiptData(json, "raw text here")
        assertNotNull(data)
        assertEquals("Alfamart Diponegoro", data!!.merchant)
        assertEquals(65000.0, data.total ?: 0.0, 0.01)
        assertEquals(Currency.IDR, data.currency)
        assertEquals("2026-08-20", data.date)
        assertEquals(6500.0, data.tax ?: 0.0, 0.01)
        assertEquals(5000.0, data.discount ?: 0.0, 0.01)
        assertEquals(3, data.items.size)
        assertTrue(data.confidence.isHighConfidence)
    }

    @Test
    fun parseReceipt_withoutApiKey_fallsBackToLocalParser() = runBlocking {
        val receiptText = """
            WARUNG PADANG JAYA
            Total: Rp 35.000
            Tanggal: 2026-08-19
        """.trimIndent()

        val result = parser.parseReceipt(receiptText)
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("WARUNG PADANG JAYA", data!!.merchant)
        assertEquals(35000.0, data.total ?: 0.0, 0.01)
    }
}
