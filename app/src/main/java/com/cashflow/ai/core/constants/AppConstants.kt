package com.cashflow.ai.core.constants

object AppConstants {
    const val DATABASE_NAME = "cashflow_database"
    const val PREFS_NAME = "cashflow_secure_prefs"

    // Default Currency
    const val DEFAULT_CURRENCY = "IDR"

    // Google Sheets
    const val DEFAULT_SHEET_NAME = "CashFlow AI - Transactions"
    const val SHEET_RANGE_ALL = "Transactions!A2:M"
    const val SHEET_HEADERS_RANGE = "Transactions!A1:M1"

    // Sync
    const val SYNC_WORK_NAME = "cashflow_periodic_sync"
    const val MAX_BATCH_SYNC_ROWS = 100

    // AI Confidence Threshold
    const val AI_HIGH_CONFIDENCE = 0.85
    const val AI_MEDIUM_CONFIDENCE = 0.70
}
