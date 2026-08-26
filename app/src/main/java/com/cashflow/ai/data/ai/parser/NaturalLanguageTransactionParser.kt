package com.cashflow.ai.data.ai.parser

import com.cashflow.ai.BuildConfig
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.domain.ai.CategoryClassifier
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.ParsedQuickTransaction
import com.cashflow.ai.domain.model.TransactionType
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.util.Locale
import java.util.regex.Pattern

class NaturalLanguageTransactionParser(
    private val categoryClassifier: CategoryClassifier,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = "gemini-1.5-flash"
) {

    suspend fun parse(input: String): List<ParsedQuickTransaction> = withContext(Dispatchers.Default) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // 1. If Gemini AI is configured, try cloud structured multi-intent extraction with 5s timeout
        if (apiKey.isNotBlank() && (trimmed.contains(" dan ") || trimmed.contains(" sama ") || trimmed.length > 30)) {
            val aiParsed = withTimeoutOrNull(6000L) {
                parseWithGemini(trimmed)
            }
            if (!aiParsed.isNullOrEmpty()) {
                return@withContext aiParsed
            }
        }

        // 2. Fast, resilient deterministic regex/NLP parser
        parseDeterministic(trimmed)
    }

    suspend fun parseDeterministic(text: String): List<ParsedQuickTransaction> {
        val results = mutableListOf<ParsedQuickTransaction>()

        // Split text by delimiters (newline, comma, semicolon, " dan ", " sama ", " plus ", " & ")
        val rawChunks = splitIntoChunks(text)

        for (rawChunk in rawChunks) {
            val chunk = rawChunk.trim()
            if (chunk.isBlank()) continue

            val parsedItem = parseSingleChunk(chunk)
            if (parsedItem != null) {
                results.add(parsedItem)
            }
        }

        return results
    }

    private suspend fun parseSingleChunk(chunk: String): ParsedQuickTransaction? {
        // 1. Detect Currency
        val currency = if (chunk.contains("$") || chunk.contains("usd", ignoreCase = true)) {
            Currency.USD
        } else {
            Currency.IDR
        }

        // 2. Extract Amount
        val amountMatch = extractAmount(chunk) ?: return null
        val amount = amountMatch.value
        val amountRawToken = amountMatch.token

        // 3. Extract Date (detect 'kemarin' / 'yesterday' / default today)
        val isYesterday = chunk.contains("kemarin", ignoreCase = true) || chunk.contains("yesterday", ignoreCase = true)
        val date = if (isYesterday) DateUtils.getYesterdayDateString() else DateUtils.today()

        // 4. Extract Description by stripping amount token and noise words
        var cleanedDesc = chunk
        if (amountRawToken.isNotBlank()) {
            cleanedDesc = cleanedDesc.replace(amountRawToken, " ")
        }
        // Remove currency words & date keywords from description
        val noiseWords = listOf(
            "rp.", "rp", "idr", "$", "usd", "kemarin", "yesterday",
            "hari ini", "today", "tadi", "beli", "bayar", "untuk", "buat"
        )
        for (nw in noiseWords) {
            cleanedDesc = cleanedDesc.replace(Regex("(?i)\\b$nw\\b"), " ")
        }
        cleanedDesc = cleanedDesc.replace(Regex("(?i)rp\\.?"), " ")
        cleanedDesc = cleanedDesc.replace(Regex("[$]"), " ")
        cleanedDesc = cleanedDesc.replace(Regex("\\s+"), " ").trim()

        val finalDescription = if (cleanedDesc.isNotBlank()) {
            cleanedDesc.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            "Quick Transaction"
        }

        // 5. Detect Transaction Type (Income vs Expense)
        val isIncome = isIncomeKeywordsPresent(chunk)
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 6. Classify Category
        val categorySuggestion = categoryClassifier.classify(
            description = finalDescription,
            merchant = finalDescription,
            amount = amount,
            type = type
        )

        return ParsedQuickTransaction(
            description = finalDescription,
            amount = amount,
            category = categorySuggestion.category,
            type = type,
            date = date,
            currency = currency,
            confidence = categorySuggestion.confidence,
            rawSnippet = chunk
        )
    }

    private fun splitIntoChunks(text: String): List<String> {
        val lines = text.split("\n", ";", ",")
        val chunks = mutableListOf<String>()

        for (line in lines) {
            // Further split by " dan " or " sama " or " plus " if multiple amounts exist in a single line
            val subChunks = line.split(Regex("(?i)\\s+(dan|sama|plus|&)\\s+"))
            for (sub in subChunks) {
                if (sub.isNotBlank()) {
                    chunks.add(sub.trim())
                }
            }
        }
        return chunks
    }

    private data class AmountExtraction(val value: Double, val token: String)

    private fun extractAmount(text: String): AmountExtraction? {
        // Pattern 1: Multipliers like 30k, 30.5k, 30rb, 30ribu, 1.5jt, 1,5jt, 1.5juta, 1.5m
        val multiplierRegex = Regex("(?i)(?:rp\\.?\\s*|[$]\\s*)?([0-9]+(?:[.,][0-9]+)?)\\s*(k|rb|ribu|jt|juta|m|mio)\\b")
        val matchMult = multiplierRegex.find(text)
        if (matchMult != null) {
            val numStr = matchMult.groupValues[1].replace(",", ".")
            val baseNum = numStr.toDoubleOrNull() ?: return null
            val unit = matchMult.groupValues[2].lowercase(Locale.ROOT)
            val mult = when (unit) {
                "k", "rb", "ribu" -> 1_000.0
                "jt", "juta", "m", "mio" -> 1_000_000.0
                else -> 1.0
            }
            return AmountExtraction(baseNum * mult, matchMult.value)
        }

        // Pattern 2: Indonesian standard with dot thousand separators e.g. 50.000 or 50.000,00 or 1.000.000
        val idDotRegex = Regex("(?i)(?:rp\\.?\\s*)?([0-9]{1,3}(?:\\.[0-9]{3})+(?:,[0-9]{1,2})?)")
        val matchDot = idDotRegex.find(text)
        if (matchDot != null) {
            val raw = matchDot.groupValues[1]
            val normalized = raw.replace(".", "").replace(",", ".")
            val value = normalized.toDoubleOrNull()
            if (value != null && value > 0.0) {
                return AmountExtraction(value, matchDot.value)
            }
        }

        // Pattern 3: US comma thousand separators e.g. 50,000 or 50,000.00 or $18.50
        val commaThousandRegex = Regex("(?i)(?:rp\\.?\\s*|[$]\\s*)?([0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,2})?)")
        val matchComma = commaThousandRegex.find(text)
        if (matchComma != null) {
            val raw = matchComma.groupValues[1]
            val normalized = raw.replace(",", "")
            val value = normalized.toDoubleOrNull()
            if (value != null && value > 0.0) {
                return AmountExtraction(value, matchComma.value)
            }
        }

        // Pattern 4: Plain numbers (e.g. 30000, 5000, 18.50)
        val plainRegex = Regex("(?i)(?:rp\\.?\\s*|[$]\\s*)?([0-9]+(?:\\.[0-9]{1,2})?)")
        val matchPlain = plainRegex.find(text)
        if (matchPlain != null) {
            val value = matchPlain.groupValues[1].toDoubleOrNull()
            if (value != null && value > 0.0) {
                return AmountExtraction(value, matchPlain.value)
            }
        }

        return null
    }

    private fun isIncomeKeywordsPresent(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val incomeKeywords = listOf(
            "gaji", "salary", "bonus", "income", "pendapatan", "terima", "dapat",
            "omset", "omzet", "dividen", "dividend", "freelance", "side gig",
            "tf masuk", "transfer masuk", "uang masuk", "penjualan", "cashback"
        )
        return incomeKeywords.any { lower.contains(it) }
    }

    private suspend fun parseWithGemini(input: String): List<ParsedQuickTransaction>? = withContext(Dispatchers.IO) {
        try {
            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )

            val prompt = """
                You are a fast personal finance transaction extraction AI.
                Extract all transactions from this natural language text:
                "$input"
                
                Current date: ${DateUtils.today()}
                Yesterday date: ${DateUtils.getYesterdayDateString()}
                
                Rules:
                - If amount mentions 'rb' or 'k', multiply by 1,000.
                - If amount mentions 'jt' or 'juta' or 'm', multiply by 1,000,000.
                - Extract individual items. Default type is EXPENSE unless text mentions salary/income/bonus.
                - Default date is today (${DateUtils.today()}) unless 'kemarin' (yesterday) is specified.
                - Assign exact category from: [Food & Dining, Groceries, Transport, Shopping, Bills & Utilities, Housing & Rent, Healthcare, Entertainment, Education, Salary, Freelance, Investment, Business Income, Other]
                
                Return ONLY a JSON Array with this exact structure:
                [
                  {
                    "description": "bensin",
                    "amount": 30000.0,
                    "category": "Transport",
                    "type": "EXPENSE",
                    "date": "YYYY-MM-DD",
                    "currency": "IDR"
                  }
                ]
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text ?: ""
            val cleaned = cleanJsonMarkdown(jsonText)
            if (cleaned.isNotBlank() && cleaned.startsWith("[")) {
                val array = JSONArray(cleaned)
                val list = mutableListOf<ParsedQuickTransaction>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val desc = obj.optString("description", "Quick Transaction")
                    val amount = obj.optDouble("amount", 0.0)
                    val cat = obj.optString("category", "Food & Dining")
                    val typeStr = obj.optString("type", "EXPENSE")
                    val date = obj.optString("date", DateUtils.today())
                    val currStr = obj.optString("currency", "IDR")

                    if (amount > 0.0) {
                        list.add(
                            ParsedQuickTransaction(
                                description = desc.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                                amount = amount,
                                category = cat,
                                type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
                                date = date,
                                currency = if (currStr.equals("USD", ignoreCase = true)) Currency.USD else Currency.IDR,
                                confidence = 0.95,
                                rawSnippet = desc
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) return@withContext list
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanJsonMarkdown(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
