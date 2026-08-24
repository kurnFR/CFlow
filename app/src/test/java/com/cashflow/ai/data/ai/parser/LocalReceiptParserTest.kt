package com.cashflow.ai.data.ai.parser

import com.cashflow.ai.domain.model.Currency
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalReceiptParserTest {

    private lateinit var parser: LocalReceiptParser

    @Before
    fun setUp() {
        parser = LocalReceiptParser()
    }

    @Test
    fun parseReceipt_indonesianIndomaretReceipt_extractsCorrectly() = runBlocking {
        val receiptText = """
            INDOMARET MERDEKA
            JL. JENDERAL SUDIRMAN NO. 45
            NPWP: 01.234.567.8-901.000
            
            19/08/2026 14:30
            KASIR: BUDI
            --------------------------------
            ROTI TAWAR SARI ROTI    15.000
            SUSU ULTRA MILK 1L      20.000
            AIR MINERAL 600ML        5.000
            --------------------------------
            SUBTOTAL                40.000
            PPN 11%                  4.400
            DISKON                   2.000
            TOTAL BAYAR             42.400
            TUNAI                   50.000
            KEMBALI                  7.600
            ================================
            TERIMA KASIH ATAS KUNJUNGAN ANDA
        """.trimIndent()

        val result = parser.parseReceipt(receiptText)
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("INDOMARET MERDEKA", data!!.merchant)
        assertEquals(42400.0, data.total ?: 0.0, 0.01)
        assertEquals(Currency.IDR, data.currency)
        assertEquals("2026-08-19", data.date)
        assertEquals(4400.0, data.tax ?: 0.0, 0.01)
        assertEquals(2000.0, data.discount ?: 0.0, 0.01)
        assertTrue(data.confidence.overall > 0.70)
    }

    @Test
    fun parseReceipt_englishStarbucksReceipt_extractsCorrectly() = runBlocking {
        val receiptText = """
            STARBUCKS COFFEE
            Store #12456 - Seattle, WA
            Tel: (206) 555-0199
            
            Date: 18 Aug 2026
            Order: #342
            
            1  Caffe Latte          $ 5.50
            1  Caramel Macchiato    $ 6.00
            1  Butter Croissant     $ 4.50
            --------------------------------
            Subtotal:               $ 16.00
            Tax:                    $ 1.60
            Amount Due:             $ 17.60
            Payment (VISA):         $ 17.60
            
            Thank You!
        """.trimIndent()

        val result = parser.parseReceipt(receiptText)
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("STARBUCKS COFFEE", data!!.merchant)
        assertEquals(17.60, data.total ?: 0.0, 0.01)
        assertEquals(Currency.USD, data.currency)
        assertEquals("2026-08-18", data.date)
        assertEquals(1.60, data.tax ?: 0.0, 0.01)
        assertTrue(data.confidence.overall > 0.70)
    }

    @Test
    fun parseReceipt_tokoMakmurPRDTestCase_extractsCorrectly() = runBlocking {
        val receiptText = """
            TOKO MAKMUR
            Jl. Merdeka No. 123
            Total: Rp 45.000
            Tax: Rp 4.500
            Date: 2026-08-19
        """.trimIndent()

        val result = parser.parseReceipt(receiptText)
        assertTrue(result.isSuccess)

        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("TOKO MAKMUR", data!!.merchant)
        assertEquals(45000.0, data.total ?: 0.0, 0.01)
        assertEquals(4500.0, data.tax ?: 0.0, 0.01)
        assertEquals("2026-08-19", data.date)
    }

    @Test
    fun parseReceipt_emptyText_returnsFailure() = runBlocking {
        val result = parser.parseReceipt("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun detectCurrency_usdSymbol_returnsUSD() {
        val currency = parser.detectCurrency("Total: $25.00")
        assertEquals(Currency.USD, currency)
    }

    @Test
    fun detectCurrency_idrText_returnsIDR() {
        val currency = parser.detectCurrency("Total: Rp 50.000")
        assertEquals(Currency.IDR, currency)
    }

    @Test
    fun parseAmountNumber_indonesianFormatted_parsesCorrectly() {
        val amt1 = parser.parseAmountNumber("Rp 1.250.000", Currency.IDR)
        assertEquals(1250000.0, amt1 ?: 0.0, 0.01)

        val amt2 = parser.parseAmountNumber("45.000,00", Currency.IDR)
        assertEquals(45000.0, amt2 ?: 0.0, 0.01)

        val amt3 = parser.parseAmountNumber("50000", Currency.IDR)
        assertEquals(50000.0, amt3 ?: 0.0, 0.01)
    }

    @Test
    fun parseAmountNumber_usdFormatted_parsesCorrectly() {
        val amt1 = parser.parseAmountNumber("$1,250.50", Currency.USD)
        assertEquals(1250.50, amt1 ?: 0.0, 0.01)

        val amt2 = parser.parseAmountNumber("18.50", Currency.USD)
        assertEquals(18.50, amt2 ?: 0.0, 0.01)
    }
}
