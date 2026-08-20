package com.cashflow.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cashflow.ai.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryExpenseRaw(
    val category: String,
    val total: Double
)

data class MonthlyTotalRaw(
    val month: String,
    val total: Double
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate
        AND (:category IS NULL OR category = :category)
        AND (:type IS NULL OR type = :type)
        AND (:searchQuery IS NULL OR description LIKE '%' || :searchQuery || '%' OR notes LIKE '%' || :searchQuery || '%')
        ORDER BY date DESC, created_at DESC
    """)
    fun getFilteredTransactions(
        startDate: String,
        endDate: String,
        category: String?,
        type: String?,
        searchQuery: String?
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE uuid = :uuid LIMIT 1")
    fun getTransactionByUuid(uuid: String): Flow<TransactionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'INCOME' AND date BETWEEN :startDate AND :endDate")
    fun getTotalIncome(startDate: String, endDate: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpense(startDate: String, endDate: String): Flow<Double>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate 
        GROUP BY category 
        ORDER BY total DESC
    """)
    fun getExpenseByCategory(startDate: String, endDate: String): Flow<List<CategoryExpenseRaw>>

    @Query("""
        SELECT strftime('%Y-%m', date) as month, SUM(amount) as total 
        FROM transactions 
        WHERE type = :type AND date BETWEEN :startDate AND :endDate 
        GROUP BY month 
        ORDER BY month ASC
    """)
    fun getMonthlyTotalsByType(type: String, startDate: String, endDate: String): Flow<List<MonthlyTotalRaw>>

    @Query("SELECT * FROM transactions WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET is_synced = 1, sync_version = :newVersion WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>, newVersion: Int)
}
