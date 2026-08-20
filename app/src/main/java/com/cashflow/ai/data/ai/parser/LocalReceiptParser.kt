package com.cashflow.ai.data.ai.parser

import android.graphics.Bitmap
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.domain.ai.ReceiptParser
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.ReceiptConfidence
import com.cashflow.ai.domain.model.ReceiptData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class LocalReceiptParser : ReceiptParser {

    override suspend fun parseReceipt(rawText: String, bitmap: Bitmap?): Result<ReceiptData> = withContext(Dispatchers.Default) {
        try {
            if (rawText.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Receipt raw text is empty"))
            }

            val lines = rawText.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val currency = detectCurrency(rawText)
            val (total, totalConfidence) = extractTotalAmount(lines, currency)
            val (merchant, merchantConfidence) = extractMerchant(lines)
            val (dateStr, dateConfidence) = extractDate(lines)
            val tax = extractTax(lines, currency)
            val discount = extractDiscount(lines, currency)
            val items = extractTopItems(lines)

            val overallConfidence = (totalConfidence * 0.45) + (merchantConfidence * 0.35) + (dateConfidence * 0.20)

            val receiptConfidence = ReceiptConfidence(
                merchant = merchantConfidence,
                total = totalConfidence,
                date = dateConfidence,
                overall = String.format(Locale.US, "%.2f", overallConfidence).toDouble()
            )

            val receiptData = ReceiptData(
                merchant = merchant,
                total = total,
                currency = currency,
                date = dateStr,
                tax = tax,
                discount = discount,
                items = items,
                confidence = receiptConfidence,
                rawText = rawText
            )

            Result.success(receiptData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun detectCurrency(text: String): Currency {
        val usdRegex = Regex("""(\$|\bUSD\b)""", RegexOption.IGNORE_CASE)
        return if (usdRegex.containsMatchIn(text)) Currency.USD else Currency.IDR
    }

    fun extractMerchant(lines: List<String>): Pair<String, Double> {
        val noiseWords = setOf(
            "STRUK", "RECEIPT", "INVOICE", "NOTA", "NOTA PEMBAYARAN", "BUKTI TRANSAKSI",
            "BILL", "SELAMAT DATANG", "WELCOME", "TERIMA KASIH", "THANK YOU", "KASIR",
            "CASHIER", "POS", "TABLE", "MEJA", "ORDER", "NO", "NPWP", "NIB", "TAX INVOICE",
            "CUSTOMER", "PELANGGAN", "MEMBER"
        )

        for (line in lines.take(6)) {
            val clean = line.replace(Regex("""[^a-zA-Z0-9\s&'.\-]"""), "").trim()
            if (clean.length < 3) continue

            val upper = clean.uppercase(Locale.ROOT)
            val isNoise = noiseWords.any { upper == it || upper.startsWith("$it ") || upper.endsWith(" $it") }
            val isAddress = upper.startsWith("JL") || upper.startsWith("JALAN") || upper.contains("TELP") || upper.contains("PHONE") || upper.contains("HTTP") || upper.contains("WWW")

            if (!isNoise && !isAddress) {
                return Pair(clean, 0.85)
            }
        }

        // Fallback: first non-empty line
        val first = lines.firstOrNull()?.replace(Regex("""[^a-zA-Z0-9\s&'.\-]"""), "")?.trim()
        return if (!first.isNullOrBlank()) {
            Pair(first, 0.50)
        } else {
            Pair("Unknown Merchant", 0.20)
        }
    }

    fun extractTotalAmount(lines: List<String>, currency: Currency): Pair<Double, Double> {
        // High priority: Grand Total, Total Bayar, Total Tagihan, Amount Due
        val highPriorityRegex = Regex(
            """(?i)(?:GRAND\s*TOTAL|TOTAL\s*BAYAR|TOTAL\s*TAGIHAN|JUMLAH\s*TOTAL|TOTAL\s*BELANJA|TOTAL\s*AKHIR|TOTAL\s*PAID|AMOUNT\s*DUE|BALANCE\s*DUE|NET\s*TOTAL)[\s:]*([^\r\n]*)"""
        )

        // Medium priority: Total, Bayar, Tagihan, Jumlah, Payment
        val mediumPriorityRegex = Regex(
            """(?i)(?:\bTOTAL\b|\bBAYAR\b|\bTAGIHAN\b|\bJUMLAH\b|\bPAYMENT\b)[\s:]*([^\r\n]*)"""
        )

        // Ignore lines that are clearly Change / Kembali / Subtotal
        val negativeKeywords = listOf("KEMBALI", "KEMBALIAN", "CHANGE", "SUBTOTAL", "SUB TOTAL", "DISKON", "DISCOUNT", "PAJAK", "TAX")

        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT)
            if (negativeKeywords.any { upper.startsWith(it) || upper.contains(" $it") }) {
                continue
            }

            highPriorityRegex.find(line)?.let { match ->
                val remainder = match.groupValues[1]
                parseAmountNumber(remainder, currency)?.let { amount ->
                    if (amount > 0.0) return Pair(amount, 0.95)
                }
            }
        }

        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT)
            if (negativeKeywords.any { upper.startsWith(it) || upper.contains(" $it") }) {
                continue
            }

            mediumPriorityRegex.find(line)?.let { match ->
                val remainder = match.groupValues[1]
                parseAmountNumber(remainder, currency)?.let { amount ->
                    if (amount > 0.0) return Pair(amount, 0.85)
                }
            }
        }

        // Fallback: look for the maximum amount detected with currency indicator
        var maxAmount = 0.0
        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT)
            if (upper.contains("KEMBALI") || upper.contains("CHANGE")) continue

            parseAmountNumber(line, currency)?.let { amt ->
                if (amt > maxAmount) {
                    maxAmount = amt
                }
            }
        }

        return if (maxAmount > 0.0) {
            Pair(maxAmount, 0.60)
        } else {
            Pair(0.0, 0.10)
        }
    }

    fun extractDate(lines: List<String>): Pair<String, Double> {
        val datePatterns = listOf(
            // YYYY-MM-DD or YYYY/MM/DD
            Regex("""\b(20\d{2})[-/.](0[1-9]|1[0-2])[-/.](0[1-9]|[12]\d|3[01])\b"""),
            // DD-MM-YYYY or DD/MM/YYYY
            Regex("""\b(0[1-9]|[12]\d|3[01])[-/.](0[1-9]|1[0-2])[-/.](20\d{2})\b"""),
            // DD-MM-YY or DD/MM/YY
            Regex("""\b(0[1-9]|[12]\d|3[01])[-/.](0[1-9]|1[0-2])[-/.]([2-3]\d)\b"""),
            // DD Mon YYYY (e.g., 19 Aug 2026, 19 Agu 2026, 19 Agustus 2026)
            Regex("""\b(0?[1-9]|[12]\d|3[01])\s+([a-zA-Z]{3,9})\s+(20\d{2})\b""")
        )

        for (line in lines) {
            // Check YYYY-MM-DD
            datePatterns[0].find(line)?.let { match ->
                val y = match.groupValues[1]
                val m = match.groupValues[2]
                val d = match.groupValues[3]
                return Pair("$y-$m-$d", 0.95)
            }

            // Check DD-MM-YYYY
            datePatterns[1].find(line)?.let { match ->
                val d = match.groupValues[1]
                val m = match.groupValues[2]
                val y = match.groupValues[3]
                return Pair("$y-$m-$d", 0.95)
            }

            // Check DD-MM-YY
            datePatterns[2].find(line)?.let { match ->
                val d = match.groupValues[1]
                val m = match.groupValues[2]
                val y = "20" + match.groupValues[3]
                return Pair("$y-$m-$d", 0.85)
            }

            // Check DD Mon YYYY
            datePatterns[3].find(line)?.let { match ->
                val d = match.groupValues[1].padStart(2, '0')
                val monStr = match.groupValues[2].lowercase(Locale.ROOT)
                val y = match.groupValues[3]
                val monthNum = parseMonthNameToNumber(monStr)
                if (monthNum != null) {
                    return Pair("$y-$monthNum-$d", 0.90)
                }
            }
        }

        // Fallback: current date
        return Pair(DateUtils.getCurrentDateString(), 0.50)
    }

    fun extractTax(lines: List<String>, currency: Currency): Double? {
        val taxRegex = Regex("""(?i)(?:TAX|PPN|PB1|PAJAK|VAT|SERVICE\s*CHARGE)[\s:]*([^\r\n]*)""")
        for (line in lines) {
            taxRegex.find(line)?.let { match ->
                val remainder = match.groupValues[1]
                parseAmountNumber(remainder, currency)?.let { amt ->
                    if (amt > 0.0) return amt
                }
            }
        }
        return null
    }

    fun extractDiscount(lines: List<String>, currency: Currency): Double? {
        val discountRegex = Regex("""(?i)(?:DISCOUNT|DISKON|POTONGAN|HEMAT|PROMO|VOUCHER)[\s:]*([^\r\n]*)""")
        for (line in lines) {
            discountRegex.find(line)?.let { match ->
                val remainder = match.groupValues[1]
                parseAmountNumber(remainder, currency)?.let { amt ->
                    if (amt > 0.0) return amt
                }
            }
        }
        return null
    }

    private fun extractTopItems(lines: List<String>): List<String> {
        val itemCandidates = mutableListOf<String>()
        val skipKeywords = setOf("TOTAL", "SUBTOTAL", "BAYAR", "KEMBALI", "TAX", "PPN", "DISKON", "STRUK", "KASIR", "TANGGAL", "TUNAI", "CHANGE", "CASH", "CARD", "DEBIT", "CREDIT")

        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT)
            val isSkip = skipKeywords.any { upper.contains(it) }
            if (!isSkip && line.length in 4..40 && !line.matches(Regex("""^[\d\s.,\-+*/:=]+$"""))) {
                itemCandidates.add(line)
                if (itemCandidates.size >= 3) break
            }
        }
        return itemCandidates
    }

    fun parseAmountNumber(text: String, currency: Currency): Double? {
        if (text.isBlank()) return null

        // Extract substring with digits and separators
        val numberRegex = Regex("""(?:Rp|IDR|\$)?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)""")
        val match = numberRegex.find(text) ?: return null
        val rawNum = match.groupValues[1]

        return try {
            if (currency == Currency.IDR) {
                // In Indonesia: 45.000 or 45.000,00 or 45000
                if (rawNum.contains(".") && rawNum.contains(",")) {
                    // 45.000,50 -> replace dot with nothing, replace comma with dot
                    val normalized = rawNum.replace(".", "").replace(",", ".")
                    normalized.toDouble()
                } else if (rawNum.contains(".")) {
                    val parts = rawNum.split(".")
                    if (parts.size > 1 && parts.last().length == 3) {
                        // Thousand separator: 45.000 or 1.250.000
                        rawNum.replace(".", "").toDouble()
                    } else if (parts.size == 2 && parts.last().length <= 2) {
                        // Decimal separator: 45.50
                        rawNum.toDouble()
                    } else {
                        rawNum.replace(".", "").toDouble()
                    }
                } else if (rawNum.contains(",")) {
                    val parts = rawNum.split(",")
                    if (parts.size > 1 && parts.last().length == 3) {
                        // Thousand separator comma: 45,000
                        rawNum.replace(",", "").toDouble()
                    } else {
                        // Decimal comma: 45,50
                        rawNum.replace(",", ".").toDouble()
                    }
                } else {
                    rawNum.toDouble()
                }
            } else {
                // USD: 1,250.50 or 18.50
                val normalized = rawNum.replace(",", "")
                normalized.toDouble()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMonthNameToNumber(monthStr: String): String? {
        return when (monthStr.lowercase(Locale.ROOT)) {
            "jan", "januari", "january" -> "01"
            "feb", "februari", "february" -> "02"
            "mar", "maret", "march" -> "03"
            "apr", "april" -> "04"
            "mei", "may" -> "05"
            "jun", "juni", "june" -> "06"
            "jul", "juli", "july" -> "07"
            "agu", "ags", "agustus", "aug", "august" -> "08"
            "sep", "september" -> "09"
            "okt", "oktober", "oct", "october" -> "10"
            "nov", "november" -> "11"
            "des", "desember", "dec", "december" -> "12"
            else -> null
        }
    }
}
