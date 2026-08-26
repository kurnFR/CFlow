package com.cashflow.ai.core.util

import com.cashflow.ai.domain.model.Currency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    fun format(amount: Double, currency: Currency = Currency.IDR, includeSign: Boolean = false): String {
        val sign = if (includeSign) {
            if (amount > 0) "+ " else if (amount < 0) "- " else ""
        } else ""

        val absAmount = abs(amount)

        val formattedNumber = when (currency) {
            Currency.IDR -> {
                val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
                    groupingSeparator = '.'
                    decimalSeparator = ','
                }
                val formatter = DecimalFormat("#,###", symbols)
                "Rp ${formatter.format(absAmount)}"
            }
            Currency.USD -> {
                val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                formatter.format(absAmount)
            }
        }

        return "$sign$formattedNumber"
    }

    fun formatCurrency(amount: Double, currency: Currency = Currency.IDR): String = format(amount, currency)

    fun formatCompact(amount: Double, currency: Currency = Currency.IDR): String {
        val absAmount = abs(amount)
        val prefix = when (currency) {
            Currency.IDR -> "Rp "
            Currency.USD -> "$"
        }

        return when {
            absAmount >= 1_000_000_000 -> {
                val formatted = String.format(Locale.US, "%.1f", absAmount / 1_000_000_000.0).replace(".0", "")
                "$prefix${formatted}B"
            }
            absAmount >= 1_000_000 -> {
                val formatted = String.format(Locale.US, "%.1f", absAmount / 1_000_000.0).replace(".0", "")
                "$prefix${formatted}M"
            }
            absAmount >= 1_000 -> {
                val formatted = String.format(Locale.US, "%.1f", absAmount / 1_000.0).replace(".0", "")
                "$prefix${formatted}K"
            }
            else -> format(amount, currency)
        }
    }
}
