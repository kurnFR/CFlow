package com.cashflow.ai.data.ai.analytics

import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionType
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

/**
 * Data-grounded financial analytics engine.
 *
 * All answers are computed deterministically from the user's own [Transaction] data.
 * No LLM calls are made here: numbers are always real, never hallucinated.
 * The LLM (when configured) is only used for re-phrasing the computed answer.
 */
class FinancialAnalyticsEngine {

    data class QueryResult(
        val answer: String,
        val isOffTopic: Boolean = false,
        val chartData: List<Pair<String, Double>> = emptyList()
    )

    data class Prediction(
        val monthLabel: String,
        val predictedExpense: Double,
        val predictedIncome: Double,
        val predictedNet: Double,
        val confidenceScore: Float, // 0f - 1f
        val trendDescription: String
    )

    // ---------- Topic gating ----------

    /**
     * Decide whether a question is about the user's finances.
     * Returns null when the query is NOT financial (and should be politely refused).
     */
    fun isFinancialQuery(query: String): Boolean {
        val q = query.lowercase(Locale.ROOT).trim()

        // Handles: spending, income, expense, category, coffee, food, transport, last month,
        // this month, total, compare, prediction, forecast, budget, salary, saved, etc.
        val financialKeywords = listOf(
            "spend", "spent", "spending", "berapa", "total", "income", "expense", "expenses",
            "category", "categories", "food", "coffee", "makan", "minum", "kopi", "bensin",
            "transport", "transportasi", "belanja", "shopping", "gaji", "salary", "budget",
            "saving", "savings", "saved", "tabungan", "menabung", "hutang", "debt", "pengeluaran",
            "pemasukan", "cash", "cashflow", "cash flow", "money", "uang", "duit", "balance",
            "saldo", "net", "profit", "loss", "last month", "this month", "last week",
            "this week", "compare", "comparison", "banding", "bandingkan", "vs", "predict",
            "prediction", "forecast", "forecasting", "proyeksi", "trend", "monthly", "bulan",
            "month", "week", "hari", "today", "hari ini", "kemarin", "yesterday", "weekly",
            "minggu", "year", "tahun", "top", "largest", "biggest", "most", "terbesar",
            "terbanyak", "frequent", "sering", "often", "count", "berapa kali", "how many",
            "how much", "berapa banyak", "average", "rata", "rata-rata", "mean", "bills",
            "tagihan", "subscription", "langganan", "rent", "sewa", "salary", "bonus"
        )

        // Strong off-topic indicators (math/calculation, weather, jokes, general chat)
        val offTopicPatterns = listOf(
            Regex("""\d+\s*[*x×]\s*\d+"""),                    // 35*56, 10 x 5
            Regex("""\d+\s*[+\-]\s*\d+"""),                    // arithmetic
            Regex("""\d+\s*/\s*\d+"""),                        // division
            Regex("""what is \d+""", RegexOption.IGNORE_CASE),
            Regex("""how (are|do|does|is|were|am)""", RegexOption.IGNORE_CASE), // conversational
            Regex("""^hi\b|^hello\b|^hey\b""", RegexOption.IGNORE_CASE),
            Regex("""joke|meme|funny|laugh""", RegexOption.IGNORE_CASE),
            Regex("""weather|temperature|forecast for|cuaca""", RegexOption.IGNORE_CASE),
            Regex("""who (are|is|made|created|wrote)""", RegexOption.IGNORE_CASE),
            Regex("""capital of|translate|define|meaning of""", RegexOption.IGNORE_CASE),
            Regex("""sports?|football|soccer|match (result|score)|skor""", RegexOption.IGNORE_CASE)
        )

        if (offTopicPatterns.any { it.containsMatchIn(q) }) {
            return false
        }

        // Financial if any keyword appears
        return financialKeywords.any { q.contains(it) }
    }

    // ---------- Query analysis ----------

    fun analyzeQuery(query: String): QueryIntent {
        val q = query.lowercase(Locale.ROOT)
        return when {
            q.contains("prediction") || q.contains("predict") || q.contains("forecast")
                || q.contains("proyeksi") || q.contains("next month")
                || q.contains("bulan depan") || q.contains("ramalan") -> QueryIntent.PREDICTION

            q.contains("compare") || q.contains("comparison") || q.contains("banding")
                || q.contains("bandingkan") || q.contains(" vs ") -> QueryIntent.COMPARE

            q.contains("top") || q.contains("largest") || q.contains("biggest")
                || q.contains("most") || q.contains("terbesar") || q.contains("terbanyak")
                || q.contains("frequent") || q.contains("sering") || q.contains("often") -> QueryIntent.TOP_CATEGORY

            q.contains("average") || q.contains("rata") || q.contains("mean") -> QueryIntent.AVERAGE

            q.contains("count") || q.contains("berapa kali") || q.contains("how many") -> QueryIntent.COUNT

            q.contains("total") || q.contains("berapa") || q.contains("how much")
                || q.contains("spend") || q.contains("spent") || q.contains("pengeluaran")
                || q.contains("income") || q.contains("pemasukan") || q.contains("gaji") -> QueryIntent.TOTAL

            else -> QueryIntent.SUMMARY
        }
    }

    enum class QueryIntent {
        TOTAL, COMPARE, TOP_CATEGORY, AVERAGE, COUNT, PREDICTION, SUMMARY
    }

    enum class TimeFrame {
        TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, ALL_TIME, LAST_MONTH_VS_THIS_MONTH
    }

    fun detectTimeFrame(query: String): TimeFrame {
        val q = query.lowercase(Locale.ROOT)
        return when {
            q.contains("last month") || q.contains("bulan lalu") || q.contains("bulan kemarin")
                || q.contains("kemarin bulan") -> TimeFrame.LAST_MONTH

            q.contains("this month") || q.contains("bulan ini") -> TimeFrame.THIS_MONTH

            q.contains("last week") || q.contains("minggu lalu") -> TimeFrame.THIS_WEEK // fallback: this week

            q.contains("this week") || q.contains("minggu ini") -> TimeFrame.THIS_WEEK

            q.contains("last 3 month") || q.contains("3 bulan") || q.contains("three month") -> TimeFrame.LAST_3_MONTHS

            q.contains("last 6 month") || q.contains("6 bulan") || q.contains("six month") -> TimeFrame.LAST_6_MONTHS

            q.contains("today") || q.contains("hari ini") -> TimeFrame.TODAY

            q.contains("all time") || q.contains("total") && q.contains("ever")
                || q.contains("semua") -> TimeFrame.ALL_TIME

            else -> TimeFrame.THIS_MONTH
        }
    }

    // ---------- Suggestion chips ----------
    fun getSuggestionChips(): List<String> = listOf(
        "How much did I spend on coffee this month?",
        "Compare last month vs this month",
        "What is my top expense category?",
        "Predict my spending for next month",
        "What's my average daily spending this week?",
        "How many transactions did I make this month?"
    )

    // ---------- Core answer computation ----------

    fun answer(
        query: String,
        transactions: List<Transaction>,
        currency: Currency = Currency.IDR
    ): QueryResult {
        if (!isFinancialQuery(query)) {
            return QueryResult(
                answer = "I'm your finance assistant — I can only answer questions about your spending, income, categories, and cash flow using your own data. 😊 For example: \"How much did I spend on coffee last month?\" or \"Predict my spending for next month.\"",
                isOffTopic = true
            )
        }

        val intent = analyzeQuery(query)
        val timeFrame = detectTimeFrame(query)
        val categoryFilter = extractCategoryFilter(query)
        val txType = detectType(query)

        return when (intent) {
            QueryIntent.PREDICTION -> {
                val prediction = predictNextMonth(transactions)
                val chart = listOf(
                    prediction.monthLabel to prediction.predictedExpense
                )
                QueryResult(
                    answer = prediction.trendDescription,
                    chartData = chart
                )
            }

            QueryIntent.COMPARE -> answerCompare(transactions, currency)

            QueryIntent.TOP_CATEGORY -> answerTopCategory(transactions, timeFrame, currency)

            QueryIntent.AVERAGE -> answerAverage(transactions, timeFrame, currency)

            QueryIntent.COUNT -> answerCount(transactions, timeFrame)

            QueryIntent.TOTAL -> answerTotal(transactions, timeFrame, categoryFilter, txType, currency)

            QueryIntent.SUMMARY -> answerSummary(transactions, timeFrame, currency)
        }
    }

    // ---------- Sub-answers ----------

    private fun answerTotal(
        transactions: List<Transaction>,
        timeFrame: TimeFrame,
        categoryFilter: String?,
        txType: TransactionType?,
        currency: Currency
    ): QueryResult {
        val filtered = filterByTimeFrame(transactions, timeFrame)
            .filter { categoryFilter == null || it.category.contains(categoryFilter, ignoreCase = true) }
            .filter { txType == null || it.type == txType }

        val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        val label = when (timeFrame) {
            TimeFrame.TODAY -> "today"
            TimeFrame.THIS_WEEK -> "this week"
            TimeFrame.LAST_MONTH -> "last month"
            TimeFrame.LAST_3_MONTHS -> "the last 3 months"
            TimeFrame.LAST_6_MONTHS -> "the last 6 months"
            TimeFrame.ALL_TIME -> "all time"
            TimeFrame.LAST_MONTH_VS_THIS_MONTH -> "this period"
            TimeFrame.THIS_MONTH -> "this month"
        }

        val filterDesc = buildString {
            if (categoryFilter != null) append(" in $categoryFilter")
            if (txType == TransactionType.INCOME) append(" income")
            if (txType == TransactionType.EXPENSE) append(" expense")
        }

        val parts = mutableListOf<String>()
        if (txType == null) {
            parts += "Your total ${if (filterDesc.isBlank()) "spending" else "spending${filterDesc}"} $label was ${formatMoney(expense, currency)}"
            parts += "Total income $label was ${formatMoney(income, currency)}"
            parts += "Net ${if (expense > income) "loss" else "surplus"}: ${formatMoney(abs(income - expense), currency)}"
        } else {
            parts += "Your total ${txType.name.lowercase()}${filterDesc} $label was ${formatMoney(if (txType == TransactionType.INCOME) income else expense, currency)}"
        }

        return QueryResult(answer = parts.joinToString("\n"))
    }

    private fun answerCompare(transactions: List<Transaction>, currency: Currency): QueryResult {
        val lastMonth = filterByTimeFrame(transactions, TimeFrame.LAST_MONTH)
        val thisMonth = filterByTimeFrame(transactions, TimeFrame.THIS_MONTH)

        val lastExpense = lastMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val thisExpense = thisMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val lastIncome = lastMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val thisIncome = thisMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        val expenseDiff = thisExpense - lastExpense
        val expenseChange = if (lastExpense > 0) (expenseDiff / lastExpense) * 100 else null

        val lines = mutableListOf(
            "📊 Last month vs this month:",
            "• Expenses: ${formatMoney(lastExpense, currency)} → ${formatMoney(thisExpense, currency)}"
        )
        if (expenseChange != null) {
            val direction = if (expenseDiff >= 0) "up" else "down"
            lines += "• That's ${direction} ${formatPercent(abs(expenseChange))}"
        }
        lines += "• Income: ${formatMoney(lastIncome, currency)} → ${formatMoney(thisIncome, currency)}"
        val netLast = lastIncome - lastExpense
        val netThis = thisIncome - thisExpense
        lines += "• Net: ${formatMoney(netLast, currency)} → ${formatMoney(netThis, currency)}"

        return QueryResult(answer = lines.joinToString("\n"))
    }

    private fun answerTopCategory(transactions: List<Transaction>, timeFrame: TimeFrame, currency: Currency): QueryResult {
        val filtered = filterByTimeFrame(transactions, timeFrame)
            .filter { it.type == TransactionType.EXPENSE }

        if (filtered.isEmpty()) {
            return QueryResult("No expense transactions found for this period.")
        }

        val byCategory = filtered.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(3)

        if (byCategory.isEmpty()) return QueryResult("No category data available yet.")

        val total = byCategory.sumOf { it.value }
        val label = when (timeFrame) {
            TimeFrame.TODAY -> "today"
            TimeFrame.THIS_WEEK -> "this week"
            TimeFrame.LAST_MONTH -> "last month"
            TimeFrame.LAST_3_MONTHS -> "the last 3 months"
            TimeFrame.LAST_6_MONTHS -> "the last 6 months"
            TimeFrame.ALL_TIME -> "all time"
            TimeFrame.LAST_MONTH_VS_THIS_MONTH -> "this period"
            TimeFrame.THIS_MONTH -> "this month"
        }

        val lines = mutableListOf<String>()
        byCategory.forEachIndexed { index, (cat, amt) ->
            val pct = if (total > 0) (amt / total) * 100 else 0.0
            val prefix = when (index) {
                0 -> "🔥 Top 1"
                1 -> "Top 2"
                else -> "Top 3"
            }
            lines += "$prefix: $cat — ${formatMoney(amt, currency)} (${formatPercent(pct)} of spending)"
        }
        return QueryResult(lines.joinToString("\n"))
    }

    private fun answerAverage(transactions: List<Transaction>, timeFrame: TimeFrame, currency: Currency): QueryResult {
        val filtered = filterByTimeFrame(transactions, timeFrame)
            .filter { it.type == TransactionType.EXPENSE }
        if (filtered.isEmpty()) return QueryResult("No expense data found for this period.")

        val avgPerTransaction = filtered.sumOf { it.amount } / filtered.size

        // avg daily: total / number of days in the period
        val days = when (timeFrame) {
            TimeFrame.TODAY -> 1
            TimeFrame.THIS_WEEK -> 7
            TimeFrame.THIS_MONTH -> Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
            TimeFrame.LAST_MONTH -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -1)
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            TimeFrame.LAST_3_MONTHS -> 90
            TimeFrame.LAST_6_MONTHS -> 182
            TimeFrame.ALL_TIME -> {
                val dates = filtered.mapNotNull { DateUtils.parseIsoDate(it.date) }
                if (dates.isEmpty()) 1 else {
                    val min = dates.minOrNull()?.time ?: 1L
                    val max = dates.maxOrNull()?.time ?: 1L
                    maxOf(1, ((max - min) / 86_400_000L).toInt() + 1)
                }
            }
            TimeFrame.LAST_MONTH_VS_THIS_MONTH -> 30
        }
        val avgDaily = filtered.sumOf { it.amount } / days

        val label = when (timeFrame) {
            TimeFrame.TODAY -> "today"
            TimeFrame.THIS_WEEK -> "this week"
            TimeFrame.LAST_MONTH -> "last month"
            TimeFrame.LAST_3_MONTHS -> "the last 3 months"
            TimeFrame.LAST_6_MONTHS -> "the last 6 months"
            TimeFrame.ALL_TIME -> "all time"
            TimeFrame.LAST_MONTH_VS_THIS_MONTH -> "this period"
            TimeFrame.THIS_MONTH -> "this month"
        }

        return QueryResult(
            "Your average expense $label:\n• Per transaction: ${formatMoney(avgPerTransaction, currency)}\n• Per day: ${formatMoney(avgDaily, currency)}"
        )
    }

    private fun answerCount(transactions: List<Transaction>, timeFrame: TimeFrame): QueryResult {
        val filtered = filterByTimeFrame(transactions, timeFrame)
        val expenseCount = filtered.count { it.type == TransactionType.EXPENSE }
        val incomeCount = filtered.count { it.type == TransactionType.INCOME }

        val label = when (timeFrame) {
            TimeFrame.TODAY -> "today"
            TimeFrame.THIS_WEEK -> "this week"
            TimeFrame.LAST_MONTH -> "last month"
            TimeFrame.LAST_3_MONTHS -> "the last 3 months"
            TimeFrame.LAST_6_MONTHS -> "the last 6 months"
            TimeFrame.ALL_TIME -> "all time"
            TimeFrame.LAST_MONTH_VS_THIS_MONTH -> "this period"
            TimeFrame.THIS_MONTH -> "this month"
        }

        return QueryResult(
            "You made $expenseCount expense${if (expenseCount == 1) "" else "s"} and $incomeCount income${if (incomeCount == 1) "" else "s"} $label."
        )
    }

    private fun answerSummary(transactions: List<Transaction>, timeFrame: TimeFrame, currency: Currency): QueryResult {
        // If it's about "how much total" with no specifics, give a broad summary
        val filtered = filterByTimeFrame(transactions, timeFrame)
        val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        val label = when (timeFrame) {
            TimeFrame.TODAY -> "today"
            TimeFrame.THIS_WEEK -> "this week"
            TimeFrame.LAST_MONTH -> "last month"
            TimeFrame.LAST_3_MONTHS -> "the last 3 months"
            TimeFrame.LAST_6_MONTHS -> "the last 6 months"
            TimeFrame.ALL_TIME -> "all time"
            TimeFrame.LAST_MONTH_VS_THIS_MONTH -> "this period"
            TimeFrame.THIS_MONTH -> "this month"
        }

        return QueryResult(
            buildString {
                append("📊 Financial summary $label:\n")
                append("• Income: ${formatMoney(income, currency)}\n")
                append("• Expenses: ${formatMoney(expense, currency)}\n")
                append("• Net: ${formatMoney(income - expense, currency)}")
            }
        )
    }

    // ---------- Prediction & regression ----------

    fun predictNextMonth(transactions: List<Transaction>, currency: Currency = Currency.IDR): Prediction {
        val expensesByMonth = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.date.take(7) } // YYYY-MM
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val incomeByMonth = transactions
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.date.take(7) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        // Sort months chronologically
        val sortedMonths = (expensesByMonth.keys + incomeByMonth.keys).distinct().sorted()

        if (sortedMonths.isEmpty()) {
            return Prediction(
                monthLabel = monthLabel(nextMonth()),
                predictedExpense = 0.0,
                predictedIncome = 0.0,
                predictedNet = 0.0,
                confidenceScore = 0f,
                trendDescription = "No transaction history yet — add some transactions and I'll predict next month's spending! 📈"
            )
        }

        // Use last 6 months for regression if available
        val window = sortedMonths.takeLast(6)
        val x = window.indices.map { it.toDouble() }
        val yExpense = window.map { expensesByMonth[it] ?: 0.0 }
        val yIncome = window.map { incomeByMonth[it] ?: 0.0 }

        val slopeExpense = linearRegressionSlope(x, yExpense)
        val slopeIncome = linearRegressionSlope(x, yIncome)
        val interceptExpense = linearRegressionIntercept(x, yExpense, slopeExpense)
        val interceptIncome = linearRegressionIntercept(x, yIncome, slopeIncome)

        // Predict next point (x = window.size)
        val predictedExpense = max(0.0, slopeExpense * window.size + interceptExpense)
        val predictedIncome = max(0.0, slopeIncome * window.size + interceptIncome)

        // Confidence: based on number of months and data regularity (R²)
        val r2Expense = rSquared(x, yExpense, slopeExpense, interceptExpense)
        val monthsCount = window.size
        val confidence = ((monthsCount / 6f) * 0.6f + r2Expense.toFloat() * 0.4f).coerceIn(0.05f, 0.95f)

        val nextMonth = nextMonth()
        val trendDir = when {
            slopeExpense > 0.03 * (window.map { expensesByMonth[it] ?: 0.0 }.average()) -> "rising"
            slopeExpense < -0.03 * (window.map { expensesByMonth[it] ?: 0.0 }.average()) -> "falling"
            else -> "stable"
        }
        val trendDesc = when (trendDir) {
            "rising" -> "Your expenses have been trending up over the last $monthsCount month(s)."
            "falling" -> "Your expenses have been trending down over the last $monthsCount month(s) — nice work! 📉"
            else -> "Your expenses have been fairly stable over the last $monthsCount month(s)."
        }

        val trendDescription = buildString {
            append("📈 Forecast for ${monthLabel(nextMonth)}:\n")
            append("• Predicted expenses: ${formatMoney(predictedExpense, currency)}\n")
            append("• Predicted income: ${formatMoney(predictedIncome, currency)}\n")
            append("• Predicted net: ${formatMoney(predictedIncome - predictedExpense, currency)}\n")
            append("• Confidence: ${formatPercent(confidence * 100.0)}\n")
            append(trendDesc)
            if (predictedIncome - predictedExpense < 0) {
                append("\n⚠️ This projection suggests a potential shortfall — consider trimming variable spending.")
            }
        }

        val predictedNet = predictedIncome - predictedExpense
        return Prediction(
            monthLabel = monthLabel(nextMonth),
            predictedExpense = predictedExpense,
            predictedIncome = predictedIncome,
            predictedNet = predictedNet,
            confidenceScore = confidence,
            trendDescription = trendDescription
        )
    }

    // ---------- Helpers ----------

    private fun nextMonth(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        return DateUtils.formatDate(cal.timeInMillis).take(7)
    }

    private fun monthLabel(month: String): String {
        return try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, month.take(4).toInt())
            cal.set(Calendar.MONTH, month.substring(5).toInt() - 1)
            java.text.SimpleDateFormat("MMM yyyy", Locale.US).format(cal.time)
        } catch (e: Exception) {
            month
        }
    }

    private fun linearRegressionSlope(x: List<Double>, y: List<Double>): Double {
        if (x.size < 2 || x.size != y.size) return 0.0
        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { (a, b) -> a * b }
        val sumXX = x.sumOf { it * it }
        val denom = n * sumXX - sumX * sumX
        if (denom == 0.0) return 0.0
        return (n * sumXY - sumX * sumY) / denom
    }

    private fun linearRegressionIntercept(x: List<Double>, y: List<Double>, slope: Double): Double {
        if (x.isEmpty()) return 0.0
        val n = x.size
        return (y.sum() - slope * x.sum()) / n
    }

    private fun rSquared(x: List<Double>, y: List<Double>, slope: Double, intercept: Double): Double {
        if (x.size < 2) return 0.0
        val meanY = y.average()
        val ssTot = y.sumOf { (it - meanY) * (it - meanY) }
        if (ssTot == 0.0) return 1.0
        val ssRes = x.zip(y).sumOf { (xi, yi) ->
            val pred = slope * xi + intercept
            (yi - pred) * (yi - pred)
        }
        return (1.0 - ssRes / ssTot).coerceIn(0.0, 1.0)
    }

    fun extractCategoryFilter(query: String): String? {
        val q = query.lowercase(Locale.ROOT)
        for (cat in COMMON_CATEGORY_WORDS) {
            if (q.contains(cat.first)) return cat.second
        }
        return null
    }

    private fun detectType(query: String): TransactionType? {
        val q = query.lowercase(Locale.ROOT)
        return when {
            q.contains("income") || q.contains("gaji") || q.contains("salary")
                || q.contains("pemasukan") || q.contains("earning") || q.contains("bonus") -> TransactionType.INCOME

            q.contains("expense") || q.contains("spend") || q.contains("spent")
                || q.contains("pengeluaran") || q.contains("belanja") -> TransactionType.EXPENSE

            else -> null
        }
    }

    private fun filterByTimeFrame(transactions: List<Transaction>, timeFrame: TimeFrame): List<Transaction> {
        val (start, end) = DateUtils.getDateRangeBounds(DateRangeFor(timeFrame))
        return transactions.filter { it.date in start..end }
    }

    private fun DateRangeFor(timeFrame: TimeFrame): com.cashflow.ai.domain.model.DateRange {
        return when (timeFrame) {
            TimeFrame.TODAY -> com.cashflow.ai.domain.model.DateRange.TODAY
            TimeFrame.THIS_WEEK -> com.cashflow.ai.domain.model.DateRange.THIS_WEEK
            TimeFrame.THIS_MONTH -> com.cashflow.ai.domain.model.DateRange.THIS_MONTH
            TimeFrame.LAST_MONTH -> com.cashflow.ai.domain.model.DateRange.LAST_MONTH
            TimeFrame.LAST_3_MONTHS -> com.cashflow.ai.domain.model.DateRange.LAST_3_MONTHS
            TimeFrame.LAST_6_MONTHS -> com.cashflow.ai.domain.model.DateRange.LAST_6_MONTHS
            TimeFrame.ALL_TIME -> com.cashflow.ai.domain.model.DateRange.ALL_TIME
            TimeFrame.LAST_MONTH_VS_THIS_MONTH -> com.cashflow.ai.domain.model.DateRange.LAST_MONTH
        }
    }

    private fun formatMoney(amount: Double, currency: Currency): String {
        val formatted = String.format(Locale.US, "%,.0f", amount)
        return when (currency) {
            Currency.USD -> "$$formatted"
            else -> "Rp $formatted"
        }
    }

    private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)

    private fun abs(value: Double): Double = if (value < 0) -value else value

    companion object {
        // Category keyword -> canonical category name (matches the app's category DB)
        private val COMMON_CATEGORY_WORDS = listOf(
            "coffee" to "Food & Dining",
            "kopi" to "Food & Dining",
            "cafe" to "Food & Dining",
            "food" to "Food & Dining",
            "makan" to "Food & Dining",
            "restaurant" to "Food & Dining",
            "grocery" to "Groceries",
            "groceries" to "Groceries",
            "belanja" to "Shopping",
            "shopping" to "Shopping",
            "transport" to "Transportation",
            "transportasi" to "Transportation",
            "bensin" to "Transportation",
            "fuel" to "Transportation",
            "gas" to "Transportation",
            "rent" to "Housing",
            "sewa" to "Housing",
            "bill" to "Utilities",
            "bills" to "Utilities",
            "tagihan" to "Utilities",
            "electric" to "Utilities",
            "internet" to "Utilities",
            "phone" to "Utilities",
            "subscription" to "Subscriptions",
            "langganan" to "Subscriptions",
            "netflix" to "Subscriptions",
            "spotify" to "Subscriptions",
            "entertainment" to "Entertainment",
            "movie" to "Entertainment",
            "film" to "Entertainment",
            "health" to "Health & Fitness",
            "medical" to "Health & Fitness",
            "treatment" to "Health & Fitness",
            "education" to "Education",
            "school" to "Education",
            "course" to "Education",
            "travel" to "Travel",
            "trip" to "Travel",
            "vacation" to "Travel"
        )
    }
}