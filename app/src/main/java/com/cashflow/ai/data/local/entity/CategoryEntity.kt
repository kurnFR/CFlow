package com.cashflow.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["type"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon")
    val icon: String = "🏷️",

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#006A6A",

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = true,

    @ColumnInfo(name = "type")
    val type: String = "EXPENSE" // EXPENSE or INCOME
)
