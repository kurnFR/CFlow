package com.cashflow.ai.domain.model

enum class AiRole { USER, ASSISTANT }

data class AiMessage(
    val id: Long,
    val role: AiRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)