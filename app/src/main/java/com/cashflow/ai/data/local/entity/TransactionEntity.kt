package com.cashflow.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["date"]),
        Index(value = ["category"]),
        Index(value = ["type"]),
        Index(value = ["is_synced"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "date")
    val date: String, // YYYY-MM-DD

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "amount")
    val amount: Double, // Always positive in entity; type specifies flow

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "type")
    val type: String, // INCOME or EXPENSE

    @ColumnInfo(name = "source")
    val source: String, // MANUAL or PHOTO

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "currency")
    val currency: String = "IDR",

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "sync_version")
    val syncVersion: Int = 1,

    @ColumnInfo(name = "ai_confidence")
    val aiConfidence: Double? = null,

    @ColumnInfo(name = "ai_merchant")
    val aiMerchant: String? = null,

    @ColumnInfo(name = "tax")
    val tax: Double? = null,

    @ColumnInfo(name = "discount")
    val discount: Double? = null,

    @ColumnInfo(name = "items_summary")
    val itemsSummary: String? = null
)
