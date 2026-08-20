package com.cashflow.ai.data.ai.parser

import android.graphics.Bitmap
import com.cashflow.ai.BuildConfig
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.domain.ai.ReceiptParser
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.ReceiptConfidence
import com.cashflow.ai.domain.model.ReceiptData
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

class GeminiReceiptParser(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = "gemini-1.5-flash",
    private val localFallbackParser: ReceiptParser = LocalReceiptParser()
) : ReceiptParser {

    override suspend fun parseReceipt(rawText: String, bitmap: Bitmap?): Result<ReceiptData> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            // No API key configured, use local heuristic parser
            return@withContext localFallbackParser.parseReceipt(rawText, bitmap)
        }

        try {
            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )

            val prompt = buildReceiptExtractionPrompt(rawText)

            val response = if (bitmap != null) {
                generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            } else {
                generativeModel.generateContent(prompt)
            }

            val responseText = response.text ?: ""
            val jsonString = cleanJsonMarkdown(responseText)

            if (jsonString.isNotBlank()) {
                val parsed = parseJsonToReceiptData(jsonString, rawText)
                if (parsed != null) {
                    return@withContext Result.success(parsed)
                }
            }

            // If JSON parsing fails, fallback to local parser
            localFallbackParser.parseReceipt(rawText, bitmap)
        } catch (e: Exception) {
            // Fallback to local parser on network or API failure
            localFallbackParser.parseReceipt(rawText, bitmap)
        }
    }

    private fun buildReceiptExtractionPrompt(rawText: String): String {
        return """
            You are a specialized receipt extraction AI. Extract key information from the provided receipt OCR text and image.
            
            Return ONLY valid JSON with this exact structure:
            {
              "merchant": "store or restaurant name",
              "total": 123.45,
              "currency": "IDR",
              "date": "YYYY-MM-DD",
              "tax": 11.50,
              "discount": 5.00,
              "items": ["item1", "item2", "item3"],
              "confidence": {
                "merchant": 0.95,
                "total": 0.98,
                "date": 0.92
              }
            }
            
            Rules:
            - If date not found, use current date (${DateUtils.getCurrentDateString()})
            - Total amount must be the final payment amount (positive numeric number)
            - If multiple totals, use the grand total
            - Recognize Indonesian receipt formats (Total, Bayar, Kembali) and English formats
            - Currency must be "IDR" or "USD"
            - Items: top 3 item descriptions
            - Confidence values must be floats between 0.0 and 1.0
            
            Receipt OCR Text:
            $rawText
        """.trimIndent()
    }

    fun cleanJsonMarkdown(rawResponse: String): String {
        var clean = rawResponse.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }

        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }

        val firstBrace = clean.indexOf('{')
        val lastBrace = clean.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            clean.substring(firstBrace, lastBrace + 1).trim()
        } else {
            clean.trim()
        }
    }

    fun parseJsonToReceiptData(jsonStr: String, rawText: String): ReceiptData? {
        return try {
            val json = JSONObject(jsonStr)

            val merchant = if (json.has("merchant") && !json.isNull("merchant")) json.getString("merchant").trim() else null
            val total = if (json.has("total") && !json.isNull("total")) json.getDouble("total") else null
            val currencyStr = if (json.has("currency") && !json.isNull("currency")) json.getString("currency").uppercase(Locale.ROOT) else "IDR"
            val currency = if (currencyStr == "USD" || currencyStr == "$") Currency.USD else Currency.IDR

            val dateStr = if (json.has("date") && !json.isNull("date")) {
                val d = json.getString("date").trim()
                if (DateUtils.isValidDate(d)) d else DateUtils.getCurrentDateString()
            } else {
                DateUtils.getCurrentDateString()
            }

            val tax = if (json.has("tax") && !json.isNull("tax")) json.getDouble("tax") else null
            val discount = if (json.has("discount") && !json.isNull("discount")) json.getDouble("discount") else null

            val items = mutableListOf<String>()
            if (json.has("items") && !json.isNull("items")) {
                val itemsArr = json.getJSONArray("items")
                for (i in 0 until itemsArr.length()) {
                    val item = itemsArr.getString(i)
                    if (item.isNotBlank()) items.add(item.trim())
                }
            }

            var merchantConf = 0.90
            var totalConf = 0.90
            var dateConf = 0.90

            if (json.has("confidence") && !json.isNull("confidence")) {
                val confObj = json.getJSONObject("confidence")
                if (confObj.has("merchant")) merchantConf = confObj.getDouble("merchant")
                if (confObj.has("total")) totalConf = confObj.getDouble("total")
                if (confObj.has("date")) dateConf = confObj.getDouble("date")
            }

            val overall = (totalConf * 0.45) + (merchantConf * 0.35) + (dateConf * 0.20)

            val confidence = ReceiptConfidence(
                merchant = merchantConf,
                total = totalConf,
                date = dateConf,
                overall = String.format(Locale.US, "%.2f", overall).toDouble()
            )

            ReceiptData(
                merchant = merchant,
                total = total,
                currency = currency,
                date = dateStr,
                tax = tax,
                discount = discount,
                items = items,
                confidence = confidence,
                rawText = rawText
            )
        } catch (e: Exception) {
            null
        }
    }
}
