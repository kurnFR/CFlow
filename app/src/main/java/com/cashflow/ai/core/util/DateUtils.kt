package com.cashflow.ai.core.util

import com.cashflow.ai.domain.model.DateRange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    init {
        isoFormat.isLenient = false
        displayFormat.isLenient = false
        monthYearFormat.isLenient = false
    }

    fun today(): String {
        return isoFormat.format(Date())
    }

    fun getCurrentDateString(): String = today()

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return isoFormat.format(cal.time)
    }

    fun parseIsoDate(isoDate: String): Date? {
        return try {
            isoFormat.parse(isoDate)
        } catch (e: Exception) {
            null
        }
    }

    fun isValidDate(dateStr: String): Boolean {
        return parseIsoDate(dateStr)?.let { isoFormat.format(it) == dateStr } ?: false
    }

    fun formatDate(timestamp: Long): String {
        return isoFormat.format(Date(timestamp))
    }

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate)
            if (date != null) displayFormat.format(date) else isoDate
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatMonthYear(dateString: String): String {
        return try {
            val date = isoFormat.parse(dateString)
            if (date != null) monthYearFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    fun getDateRangeBounds(dateRange: DateRange): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val today = isoFormat.format(calendar.time)

        return when (dateRange) {
            DateRange.TODAY -> Pair(today, today)

            DateRange.THIS_WEEK -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = isoFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val end = isoFormat.format(calendar.time)
                Pair(start, end)
            }

            DateRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = isoFormat.format(calendar.time)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = isoFormat.format(calendar.time)
                Pair(start, end)
            }

            DateRange.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = isoFormat.format(calendar.time)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = isoFormat.format(calendar.time)
                Pair(start, end)
            }

            DateRange.LAST_3_MONTHS -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = isoFormat.format(calendar.time)
                Pair(start, today)
            }

            DateRange.LAST_6_MONTHS -> {
                calendar.add(Calendar.MONTH, -6)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = isoFormat.format(calendar.time)
                Pair(start, today)
            }

            DateRange.CUSTOM -> Pair("1970-01-01", "2099-12-31")
        }
    }
}
