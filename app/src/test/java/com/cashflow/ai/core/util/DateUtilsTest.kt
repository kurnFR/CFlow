package com.cashflow.ai.core.util

import com.cashflow.ai.domain.model.DateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun testTodayFormat() {
        val today = DateUtils.today()
        assertNotNull(today)
        assertTrue(today.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun testDateRangeBoundsToday() {
        val (start, end) = DateUtils.getDateRangeBounds(DateRange.TODAY)
        assertEquals(start, end)
        assertEquals(DateUtils.today(), start)
    }

    @Test
    fun testDateRangeBoundsThisMonth() {
        val (start, end) = DateUtils.getDateRangeBounds(DateRange.THIS_MONTH)
        assertTrue(start.endsWith("-01"))
        assertTrue(start <= end)
    }

    @Test
    fun invalidIsoDateIsRejected() {
        assertTrue(!DateUtils.isValidDate("2026-02-30"))
        assertTrue(!DateUtils.isValidDate("2026-2-03"))
    }
}
