package com.cashflow.ai.data.mapper

import com.cashflow.ai.data.local.entity.CategoryEntity
import com.cashflow.ai.domain.model.Category
import com.cashflow.ai.domain.model.TransactionType

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        isDefault = isDefault,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE }
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        isDefault = isDefault,
        type = type.name
    )
}
