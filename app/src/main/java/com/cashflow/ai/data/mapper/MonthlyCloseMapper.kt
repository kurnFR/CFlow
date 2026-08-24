package com.cashflow.ai.data.mapper

import com.cashflow.ai.data.local.entity.MonthlyCloseEntity
import com.cashflow.ai.domain.model.MonthlyClose

fun MonthlyCloseEntity.toDomain() = MonthlyClose(
    month = month,
    closedAt = closedAt,
    income = income,
    expense = expense,
    net = net,
    topExpenseCategory = topExpenseCategory,
    insight = insight,
    generatedAt = generatedAt,
    isAiGenerated = isAiGenerated
)

fun MonthlyClose.toEntity() = MonthlyCloseEntity(
    month = month,
    closedAt = closedAt,
    income = income,
    expense = expense,
    net = net,
    topExpenseCategory = topExpenseCategory,
    insight = insight,
    generatedAt = generatedAt,
    isAiGenerated = isAiGenerated
)
