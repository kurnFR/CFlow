package com.cashflow.ai.core.util

import com.cashflow.ai.domain.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testFormatIDR() {
        val amount = 75000.0
        val formatted = CurrencyFormatter.format(amount, Currency.IDR)
        assertTrue(formatted.contains("75.000") || formatted.contains("75,000"))
        assertTrue(formatted.startsWith("Rp"))
    }

    @Test
    fun testFormatUSD() {
        val amount = 18.50
        val formatted = CurrencyFormatter.format(amount, Currency.USD)
        assertTrue(formatted.contains("18.50"))
        assertTrue(formatted.contains("$"))
    }

    @Test
    fun testFormatCompact() {
        val million = 8500000.0
        val formatted = CurrencyFormatter.formatCompact(million, Currency.IDR)
        assertEquals("Rp 8.5M", formatted)

        val thousand = 25000.0
        val formattedK = CurrencyFormatter.formatCompact(thousand, Currency.IDR)
        assertEquals("Rp 25K", formattedK)
    }

    @Test
    fun testFormatWithSign() {
        val positive = 100000.0
        val negative = -50000.0
        val posFormatted = CurrencyFormatter.format(positive, Currency.IDR, includeSign = true)
        val negFormatted = CurrencyFormatter.format(negative, Currency.IDR, includeSign = true)

        assertTrue(posFormatted.startsWith("+"))
        assertTrue(negFormatted.startsWith("-"))
    }
}
