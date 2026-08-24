package com.cashflow.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_closes")
data class MonthlyCloseEntity(
    @PrimaryKey
    @ColumnInfo(name = "month")
    val month: String,
    @ColumnInfo(name = "closed_at")
    val closedAt: Long? = null,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0,
    @ColumnInfo(name = "top_expense_category")
    val topExpenseCategory: String? = null,
    val insight: String = "",
    @ColumnInfo(name = "generated_at")
    val generatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_ai_generated")
    val isAiGenerated: Boolean = false
)
