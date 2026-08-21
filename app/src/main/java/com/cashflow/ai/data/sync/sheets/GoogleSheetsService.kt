package com.cashflow.ai.data.sync.sheets

import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.sync.SpreadsheetInfo

interface GoogleSheetsService {
    suspend fun createSpreadsheet(title: String): Result<SpreadsheetInfo>
    suspend fun listSpreadsheets(): Result<List<SpreadsheetInfo>>
    suspend fun verifyAndSetupHeaders(spreadsheetId: String): Result<Unit>
    suspend fun appendTransactions(spreadsheetId: String, transactions: List<Transaction>): Result<Int>
    suspend fun readAllTransactions(spreadsheetId: String): Result<List<Transaction>>
}
