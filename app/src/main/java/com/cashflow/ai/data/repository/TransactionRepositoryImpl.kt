package com.cashflow.ai.data.repository

import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.data.local.dao.CategoryDao
import com.cashflow.ai.data.local.dao.TransactionDao
import com.cashflow.ai.data.local.entity.CategoryEntity
import com.cashflow.ai.data.mapper.toDomain
import com.cashflow.ai.data.mapper.toEntity
import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.CategoryExpense
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.MonthlyClose
import com.cashflow.ai.domain.model.MonthlyTotal
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSummary
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getTransactions(
        dateRange: DateRange,
        category: String?,
        type: TransactionType?,
        searchQuery: String?
    ): Flow<List<Transaction>> {
        val (startDate, endDate) = DateUtils.getDateRangeBounds(dateRange)
        val formattedSearch = if (searchQuery.isNullOrBlank()) null else searchQuery.trim()

        return transactionDao.getFilteredTransactions(
            startDate = startDate,
            endDate = endDate,
            category = category,
            type = type?.name,
            searchQuery = formattedSearch
        )
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getTransactionById(id: Long): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)
    }

    override fun getTransactionByUuid(uuid: String): Flow<Transaction?> {
        return transactionDao.getTransactionByUuid(uuid)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun insertTransaction(transaction: Transaction): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val entity = transaction.toEntity().copy(
                updatedAt = System.currentTimeMillis()
            )
            val id = transactionDao.insertTransaction(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = transaction.toEntity().copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            transactionDao.updateTransaction(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteTransaction(transaction.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransactionById(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteTransactionById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSummary(dateRange: DateRange): Flow<TransactionSummary> {
        val (startDate, endDate) = DateUtils.getDateRangeBounds(dateRange)
        val incomeFlow = transactionDao.getTotalIncome(startDate, endDate)
        val expenseFlow = transactionDao.getTotalExpense(startDate, endDate)

        return combine(incomeFlow, expenseFlow) { income, expense ->
            TransactionSummary(
                totalIncome = income,
                totalExpense = expense,
                netBalance = income - expense
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getCategoryExpenses(dateRange: DateRange): Flow<List<CategoryExpense>> {
        val (startDate, endDate) = DateUtils.getDateRangeBounds(dateRange)
        val rawExpensesFlow = transactionDao.getExpenseByCategory(startDate, endDate)
        val categoriesFlow = categoryDao.getAllCategories()

        return combine(rawExpensesFlow, categoriesFlow) { rawList, categories ->
            val totalExpenseSum = rawList.sumOf { it.total }
            val categoryMap = categories.associateBy { it.name }

            rawList.map { raw ->
                val meta = categoryMap[raw.category]
                val percentage = if (totalExpenseSum > 0) ((raw.total / totalExpenseSum) * 100).toFloat() else 0f
                CategoryExpense(
                    category = raw.category,
                    total = raw.total,
                    percentage = percentage,
                    icon = meta?.icon ?: "🏷️",
                    colorHex = meta?.colorHex ?: "#006A6A"
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getMonthlyTrends(dateRange: DateRange): Flow<List<MonthlyTotal>> {
        val (startDate, endDate) = DateUtils.getDateRangeBounds(dateRange)
        val incomeFlow = transactionDao.getMonthlyTotalsByType(TransactionType.INCOME.name, startDate, endDate)
        val expenseFlow = transactionDao.getMonthlyTotalsByType(TransactionType.EXPENSE.name, startDate, endDate)

        return combine(incomeFlow, expenseFlow) { incomeList, expenseList ->
            val months = (incomeList.map { it.month } + expenseList.map { it.month }).distinct().sorted()
            val incomeMap = incomeList.associate { it.month to it.total }
            val expenseMap = expenseList.associate { it.month to it.total }

            months.map { month ->
                val inc = incomeMap[month] ?: 0.0
                val exp = expenseMap[month] ?: 0.0
                MonthlyTotal(
                    month = month,
                    incomeTotal = inc,
                    expenseTotal = exp,
                    netTotal = inc - exp
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getUnsyncedTransactions(): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getUnsyncedTransactions().map { it.toDomain() }
    }

    override suspend fun markAsSynced(transactionIds: List<Long>, newVersion: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.markAsSynced(transactionIds, newVersion)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun upsertRemoteTransactions(transactions: List<Transaction>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entities = transactions.map { it.toEntity() }
            transactionDao.insertTransactions(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.name)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun insertCategory(category: Category): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = categoryDao.insertCategory(category.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun seedDefaultCategoriesIfEmpty() = withContext(Dispatchers.IO) {
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Food & Dining", icon = "🍔", colorHex = "#FF5722", type = "EXPENSE"),
                CategoryEntity(name = "Groceries", icon = "🛒", colorHex = "#4CAF50", type = "EXPENSE"),
                CategoryEntity(name = "Restaurant", icon = "🍽️", colorHex = "#FF9800", type = "EXPENSE"),
                CategoryEntity(name = "Coffee", icon = "☕", colorHex = "#795548", type = "EXPENSE"),
                CategoryEntity(name = "Transport", icon = "🚗", colorHex = "#2196F3", type = "EXPENSE"),
                CategoryEntity(name = "Fuel", icon = "⛽", colorHex = "#00BCD4", type = "EXPENSE"),
                CategoryEntity(name = "Ride Hailing", icon = "🛵", colorHex = "#009688", type = "EXPENSE"),
                CategoryEntity(name = "Shopping", icon = "🛍️", colorHex = "#E91E63", type = "EXPENSE"),
                CategoryEntity(name = "Bills & Utilities", icon = "💡", colorHex = "#607D8B", type = "EXPENSE"),
                CategoryEntity(name = "Housing & Rent", icon = "🏢", colorHex = "#5D4037", type = "EXPENSE"),
                CategoryEntity(name = "Healthcare", icon = "💊", colorHex = "#F44336", type = "EXPENSE"),
                CategoryEntity(name = "Entertainment", icon = "🎬", colorHex = "#9E9E9E", type = "EXPENSE"),
                CategoryEntity(name = "Education", icon = "📚", colorHex = "#3F51B5", type = "EXPENSE"),
                CategoryEntity(name = "Salary", icon = "💼", colorHex = "#4CAF50", type = "INCOME"),
                CategoryEntity(name = "Freelance", icon = "💻", colorHex = "#00BCD4", type = "INCOME"),
                CategoryEntity(name = "Investment", icon = "📈", colorHex = "#8BC34A", type = "INCOME"),
                CategoryEntity(name = "Business Income", icon = "🏪", colorHex = "#FF9800", type = "INCOME")
            )
            categoryDao.insertCategories(defaultCategories)
        }
    }

    override suspend fun getMostFrequentCategoryForQuery(query: String): String? = withContext(Dispatchers.IO) {
        if (query.isBlank()) null else transactionDao.getMostFrequentCategoryForQuery(query.trim())
    }

    override fun getLatestMonthlyClose(): Flow<MonthlyClose?> {
        return transactionDao.getLatestMonthlyClose()
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun saveMonthlyClose(monthlyClose: MonthlyClose): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.upsertMonthlyClose(monthlyClose.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
