package com.cashflow.ai.domain.model.sync

data class SyncResult(
    val isSuccess: Boolean,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val imagesUploadedCount: Int = 0,
    val conflictsResolvedCount: Int = 0,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
