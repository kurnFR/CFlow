package com.cashflow.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cashflow.ai.core.constants.AppConstants
import com.cashflow.ai.data.local.converter.DateConverters
import com.cashflow.ai.data.local.dao.CategoryDao
import com.cashflow.ai.data.local.dao.TransactionDao
import com.cashflow.ai.data.local.entity.CategoryEntity
import com.cashflow.ai.data.local.entity.TransactionEntity
import com.cashflow.ai.data.local.entity.MonthlyCloseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        MonthlyCloseEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class CashFlowDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: CashFlowDatabase? = null

        fun getInstance(context: Context): CashFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CashFlowDatabase::class.java,
                    AppConstants.DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS monthly_closes (
                        month TEXT NOT NULL PRIMARY KEY,
                        closed_at INTEGER,
                        income REAL NOT NULL,
                        expense REAL NOT NULL,
                        net REAL NOT NULL,
                        top_expense_category TEXT,
                        insight TEXT NOT NULL,
                        generated_at INTEGER NOT NULL,
                        is_ai_generated INTEGER NOT NULL
                    )"""
                )
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDefaultCategories(database.categoryDao())
                    }
                }
            }

            private suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
                val defaultCategories = listOf(
                    // Expense Categories
                    CategoryEntity(name = "Food & Dining", icon = "🍔", colorHex = "#FF5722", type = "EXPENSE"),
                    CategoryEntity(name = "Groceries", icon = "🛒", colorHex = "#4CAF50", type = "EXPENSE"),
                    CategoryEntity(name = "Restaurant", icon = "🍽️", colorHex = "#FF9800", type = "EXPENSE"),
                    CategoryEntity(name = "Coffee", icon = "☕", colorHex = "#795548", type = "EXPENSE"),
                    CategoryEntity(name = "Transport", icon = "🚗", colorHex = "#2196F3", type = "EXPENSE"),
                    CategoryEntity(name = "Fuel", icon = "⛽", colorHex = "#00BCD4", type = "EXPENSE"),
                    CategoryEntity(name = "Public Transport", icon = "🚌", colorHex = "#03A9F4", type = "EXPENSE"),
                    CategoryEntity(name = "Ride Hailing", icon = "🛵", colorHex = "#009688", type = "EXPENSE"),
                    CategoryEntity(name = "Shopping", icon = "🛍️", colorHex = "#E91E63", type = "EXPENSE"),
                    CategoryEntity(name = "Clothing", icon = "👕", colorHex = "#9C27B0", type = "EXPENSE"),
                    CategoryEntity(name = "Electronics", icon = "📱", colorHex = "#673AB7", type = "EXPENSE"),
                    CategoryEntity(name = "Home Goods", icon = "🏠", colorHex = "#3F51B5", type = "EXPENSE"),
                    CategoryEntity(name = "Bills & Utilities", icon = "💡", colorHex = "#607D8B", type = "EXPENSE"),
                    CategoryEntity(name = "Electricity", icon = "⚡", colorHex = "#FFEB3B", type = "EXPENSE"),
                    CategoryEntity(name = "Water", icon = "💧", colorHex = "#00E5FF", type = "EXPENSE"),
                    CategoryEntity(name = "Internet & Phone", icon = "📶", colorHex = "#8BC34A", type = "EXPENSE"),
                    CategoryEntity(name = "Housing & Rent", icon = "🏢", colorHex = "#5D4037", type = "EXPENSE"),
                    CategoryEntity(name = "Healthcare", icon = "💊", colorHex = "#F44336", type = "EXPENSE"),
                    CategoryEntity(name = "Fitness", icon = "🏋️", colorHex = "#CDDC39", type = "EXPENSE"),
                    CategoryEntity(name = "Entertainment", icon = "🎬", colorHex = "#9E9E9E", type = "EXPENSE"),
                    CategoryEntity(name = "Education", icon = "📚", colorHex = "#3F51B5", type = "EXPENSE"),
                    CategoryEntity(name = "Other Expense", icon = "🏷️", colorHex = "#757575", type = "EXPENSE"),

                    // Income Categories
                    CategoryEntity(name = "Salary", icon = "💼", colorHex = "#4CAF50", type = "INCOME"),
                    CategoryEntity(name = "Freelance", icon = "💻", colorHex = "#00BCD4", type = "INCOME"),
                    CategoryEntity(name = "Investment", icon = "📈", colorHex = "#8BC34A", type = "INCOME"),
                    CategoryEntity(name = "Business Income", icon = "🏪", colorHex = "#FF9800", type = "INCOME"),
                    CategoryEntity(name = "Bonus & Gifts", icon = "🎁", colorHex = "#E91E63", type = "INCOME"),
                    CategoryEntity(name = "Other Income", icon = "💰", colorHex = "#2E7D32", type = "INCOME")
                )
                categoryDao.insertCategories(defaultCategories)
            }
        }
    }
}
