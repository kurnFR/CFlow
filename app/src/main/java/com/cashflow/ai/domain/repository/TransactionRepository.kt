package com.cashflow.ai.domain.repository

import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.CategoryExpense
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.MonthlyClose
import com.cashflow.ai.domain.model.MonthlyTotal
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSummary
import com.cashflow.ai.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>

    fun getTransactions(
        dateRange: DateRange = DateRange.THIS_MONTH,
        startDate: String? = null,
        endDate: String? = null,
        category: String? = null,
        type: TransactionType? = null,
        searchQuery: String? = null
    ): Flow<List<Transaction>>

    fun getTransactionById(id: Long): Flow<Transaction?>

    fun getTransactionByUuid(uuid: String): Flow<Transaction?>

    suspend fun insertTransaction(transaction: Transaction): Result<Long>

    suspend fun insertTransactions(transactions: List<Transaction>): Result<List<Long>>

    suspend fun updateTransaction(transaction: Transaction): Result<Unit>

    suspend fun deleteTransaction(transaction: Transaction): Result<Unit>

    suspend fun deleteTransactionById(id: Long): Result<Unit>

    fun getSummary(dateRange: DateRange = DateRange.THIS_MONTH): Flow<TransactionSummary>

    fun getCategoryExpenses(dateRange: DateRange = DateRange.THIS_MONTH): Flow<List<CategoryExpense>>

    fun getMonthlyTrends(dateRange: DateRange = DateRange.LAST_6_MONTHS): Flow<List<MonthlyTotal>>

    suspend fun getUnsyncedTransactions(): List<Transaction>

    suspend fun markAsSynced(transactionIds: List<Long>, newVersion: Int): Result<Unit>

    suspend fun upsertRemoteTransactions(transactions: List<Transaction>): Result<Unit>

    fun getAllCategories(): Flow<List<Category>>

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    suspend fun insertCategory(category: Category): Result<Long>

    suspend fun seedDefaultCategoriesIfEmpty()

    suspend fun getMostFrequentCategoryForQuery(query: String): String?

    fun getLatestMonthlyClose(): Flow<MonthlyClose?>

    suspend fun saveMonthlyClose(monthlyClose: MonthlyClose): Result<Unit>
}
