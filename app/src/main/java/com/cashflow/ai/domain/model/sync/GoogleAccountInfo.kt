package com.cashflow.ai.domain.model.sync

data class GoogleAccountInfo(
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val spreadsheetId: String? = null,
    val spreadsheetName: String? = null,
    val isConnected: Boolean = true
)
