package com.cashflow.ai.data.ai.category

import com.cashflow.ai.BuildConfig
import com.cashflow.ai.domain.ai.CategoryClassifier
import com.cashflow.ai.domain.model.CategorySource
import com.cashflow.ai.domain.model.CategorySuggestion
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.repository.TransactionRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class SmartCategoryClassifier(
    private val transactionRepository: TransactionRepository? = null,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = "gemini-1.5-flash"
) : CategoryClassifier {

    private val rules: List<CategoryRule> = listOf(
        // Food & Dining
        CategoryRule(
            category = "Food & Dining",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "makan", "restoran", "restaurant", "cafe", "kafe", "kopi", "coffee",
                "starbucks", "mcdonald", "mcd", "kfc", "burger king", "hokben", "gofood",
                "grabfood", "shopeefood", "warung", "bakso", "mie ayam", "sate", "nasgor",
                "nasi goreng", "padang", "lunch", "dinner", "breakfast", "pizza", "dominos",
                "chatime", "mixue", "janji jiwa", "kopi kenangan", "fore coffee", "solaria",
                "d'cost", "richeese", "ayam geprek", "seblak", "roti", "bakery", "bread"
            )
        ),
        // Groceries
        CategoryRule(
            category = "Groceries",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "groceries", "supermarket", "minimarket", "indomaret", "alfamart", "alfamidi",
                "hypermart", "superindo", "transmart", "lotte mart", "hero", "pasar",
                "sembako", "sayur", "buah", "daging", "beras", "minyak goreng", "telur",
                "sayuran", "bumbu", "toko kelontong", "mart"
            )
        ),
        // Transport
        CategoryRule(
            category = "Transport",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "transport", "transportasi", "gojek", "goride", "gocar", "grab", "grabcar",
                "grabride", "maxim", "bluebird", "taksi", "taxi", "bensin", "bbm", "pertamina",
                "shell", "bp", "vivo", "spbu", "parkir", "parking", "tol", "toll", "krl",
                "mrt", "lrt", "transjakarta", "busway", "kereta", "kai", "tiket pesawat",
                "garuda", "lion air", "citilink", "ojek", "service motor", "bengkel", "oli"
            )
        ),
        // Shopping
        CategoryRule(
            category = "Shopping",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "shopping", "belanja", "shopee", "tokopedia", "lazada", "blibli",
                "tiktok shop", "zalora", "uniqlo", "zara", "h&m", "pull&bear", "matahari",
                "miniso", "ikea", "ace hardware", "gramedia", "sepatu", "baju", "celana",
                "kaos", "electronics", "gadget", "mall", "department store", "boutique"
            )
        ),
        // Bills & Utilities
        CategoryRule(
            category = "Bills & Utilities",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "bills", "tagihan", "pln", "listrik", "token listrik", "pdam", "air",
                "indihome", "biznet", "first media", "myrepublic", "telkomsel", "indosat",
                "xl axiata", "tri", "smartfren", "pulsa", "paket data", "wifi", "internet",
                "bpjs", "bpjs kesehatan", "pbb", "iuran", "gas", "pgn"
            )
        ),
        // Housing & Rent
        CategoryRule(
            category = "Housing & Rent",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "rent", "sewa", "kontrakan", "kost", "kosan", "apartemen", "apartment",
                "ipl", "maintenance", "cicilan rumah", "kpr", "properti", "perumahan"
            )
        ),
        // Healthcare
        CategoryRule(
            category = "Healthcare",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "healthcare", "kesehatan", "apotek", "pharmacy", "kimia farma", "century",
                "guardian", "k24", "obat", "dokter", "doctor", "klinik", "clinic",
                "rumah sakit", "hospital", "dental", "gigi", "lab", "vitamin", "gym",
                "fitness", "optik", "kacamata", "medis"
            )
        ),
        // Entertainment
        CategoryRule(
            category = "Entertainment",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "entertainment", "hiburan", "cinema xxi", "cgv", "cinepolis", "bioskop",
                "netflix", "spotify", "youtube premium", "disney", "game", "steam",
                "playstation", "nintendo", "mobile legends", "top up game", "karaoke",
                "billiard", "wisata", "rekreasi", "taman", "museum", "konser"
            )
        ),
        // Education
        CategoryRule(
            category = "Education",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "education", "pendidikan", "kursus", "course", "training", "udemy",
                "coursera", "ruangguru", "bimbel", "kuliah", "spp", "buku", "book",
                "seminar", "workshop", "sekolah", "les"
            )
        ),
        // Salary (Income)
        CategoryRule(
            category = "Salary",
            type = TransactionType.INCOME,
            keywords = listOf(
                "salary", "gaji", "payroll", "upah", "honor", "bonus", "thr",
                "insentif", "monthly salary", "gajian"
            )
        ),
        // Freelance (Income)
        CategoryRule(
            category = "Freelance",
            type = TransactionType.INCOME,
            keywords = listOf(
                "freelance", "proyek", "project", "client payment", "upwork", "fiverr",
                "jasadesain", "konsultan", "pembayaran freelance", "side gig"
            )
        ),
        // Investment (Income)
        CategoryRule(
            category = "Investment",
            type = TransactionType.INCOME,
            keywords = listOf(
                "investment", "investasi", "dividen", "dividend", "saham", "stock",
                "reksadana", "mutual fund", "bibit", "ajaib", "bareksa", "crypto",
                "binance", "tokocrypto", "bunga deposito", "interest", "capital gain"
            )
        ),
        // Business Income (Income)
        CategoryRule(
            category = "Business Income",
            type = TransactionType.INCOME,
            keywords = listOf(
                "business income", "pendapatan usaha", "omset", "penjualan", "sales",
                "warung income", "toko profit", "revenue", "hasil penjualan"
            )
        )
    )

    override suspend fun classify(
        description: String,
        merchant: String?,
        amount: Double?,
        type: TransactionType
    ): CategorySuggestion = withContext(Dispatchers.Default) {
        val queryText = listOfNotNull(merchant, description).joinToString(" ").trim()
        if (queryText.isBlank()) {
            return@withContext getDefaultCategory(type)
        }

        // Level 1: Keyword & Pattern Matching
        val ruleResult = matchByRules(queryText, type)
        if (ruleResult != null && ruleResult.confidence >= 0.85) {
            return@withContext ruleResult
        }

        // Level 2: User Transaction History
        if (transactionRepository != null) {
            try {
                val historicalCategory = transactionRepository.getMostFrequentCategoryForQuery(queryText)
                if (!historicalCategory.isNullOrBlank()) {
                    return@withContext CategorySuggestion(
                        category = historicalCategory,
                        confidence = 0.90,
                        source = CategorySource.HISTORY
                    )
                }
            } catch (e: Exception) {
                // Ignore and proceed to next level
            }
        }

        // Return rule result if we have a moderate confidence match
        if (ruleResult != null && ruleResult.confidence >= 0.70) {
            return@withContext ruleResult
        }

        // Level 3: Gemini AI Cloud Classifier
        if (apiKey.isNotBlank()) {
            val aiResult = classifyWithGemini(queryText, amount, type)
            if (aiResult != null) {
                return@withContext aiResult
            }
        }

        // Level 4: Fallback
        ruleResult ?: getDefaultCategory(type)
    }

    private fun matchByRules(queryText: String, type: TransactionType): CategorySuggestion? {
        val lower = queryText.lowercase(Locale.ROOT)
        var bestCategory: String? = null
        var maxScore = 0.0

        for (rule in rules) {
            // Give preference to matching transaction type
            val typeMultiplier = if (rule.type == type) 1.0 else 0.6

            for (kw in rule.keywords) {
                if (lower == kw) {
                    val score = 0.95 * typeMultiplier
                    if (score > maxScore) {
                        maxScore = score
                        bestCategory = rule.category
                    }
                } else if (lower.contains(kw)) {
                    val score = 0.85 * typeMultiplier
                    if (score > maxScore) {
                        maxScore = score
                        bestCategory = rule.category
                    }
                }
            }
        }

        return if (bestCategory != null) {
            CategorySuggestion(
                category = bestCategory,
                confidence = maxScore,
                source = CategorySource.RULE
            )
        } else {
            null
        }
    }

    private suspend fun classifyWithGemini(
        queryText: String,
        amount: Double?,
        type: TransactionType
    ): CategorySuggestion? = withContext(Dispatchers.IO) {
        try {
            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )

            val prompt = """
                You are a category classification AI for personal finance.
                
                Classify the following transaction into one of these exact categories:
                [Food & Dining, Groceries, Transport, Shopping, Bills & Utilities, Housing & Rent, Healthcare, Entertainment, Education, Salary, Freelance, Investment, Business Income, Other]
                
                Transaction Type: ${type.name}
                Transaction Details: "$queryText"
                Amount: ${amount ?: "N/A"}
                
                Return ONLY the category name and nothing else.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val resultCategory = response.text?.trim()?.replace("\"", "")?.replace("'", "") ?: ""

            val validCategories = setOf(
                "Food & Dining", "Groceries", "Transport", "Shopping", "Bills & Utilities",
                "Housing & Rent", "Healthcare", "Entertainment", "Education", "Salary",
                "Freelance", "Investment", "Business Income", "Other"
            )

            val matched = validCategories.firstOrNull { it.equals(resultCategory, ignoreCase = true) }
            if (matched != null) {
                CategorySuggestion(
                    category = matched,
                    confidence = 0.88,
                    source = CategorySource.AI
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultCategory(type: TransactionType): CategorySuggestion {
        return if (type == TransactionType.INCOME) {
            CategorySuggestion(
                category = "Salary",
                confidence = 0.50,
                source = CategorySource.DEFAULT
            )
        } else {
            CategorySuggestion(
                category = "Food & Dining",
                confidence = 0.50,
                source = CategorySource.DEFAULT
            )
        }
    }

    private data class CategoryRule(
        val category: String,
        val type: TransactionType,
        val keywords: List<String>
    )
}
