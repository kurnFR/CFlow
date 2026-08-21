package com.cashflow.ai.data.sync.sheets

import android.content.Context
import com.cashflow.ai.core.constants.AppConstants
import com.cashflow.ai.data.sync.auth.GoogleAuthManager
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.sync.SpreadsheetInfo
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSheetsServiceImpl(
    private val context: Context,
    private val authManager: GoogleAuthManager
) : GoogleSheetsService {

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    private fun getSheetsClient(): Sheets? {
        val account = authManager.getSignedInAccount() ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        return Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("CashFlow AI")
            .build()
    }

    private fun getDriveClient(): Drive? {
        val account = authManager.getSignedInAccount() ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("CashFlow AI")
            .build()
    }

    override suspend fun createSpreadsheet(title: String): Result<SpreadsheetInfo> = withContext(Dispatchers.IO) {
        try {
            val sheetsClient = getSheetsClient()
                ?: return@withContext Result.failure(IllegalStateException("User is not signed in to Google"))

            val spreadsheet = Spreadsheet().apply {
                properties = SpreadsheetProperties().apply {
                    this.title = title
                }
            }

            val created = sheetsClient.spreadsheets().create(spreadsheet).execute()
            val spreadsheetId = created.spreadsheetId
            val spreadsheetUrl = created.spreadsheetUrl ?: "https://docs.google.com/spreadsheets/d/$spreadsheetId/edit"

            // Setup Header Row
            verifyAndSetupHeaders(spreadsheetId)

            val info = SpreadsheetInfo(
                id = spreadsheetId,
                name = title,
                url = spreadsheetUrl
            )
            authManager.setSpreadsheet(spreadsheetId, title)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listSpreadsheets(): Result<List<SpreadsheetInfo>> = withContext(Dispatchers.IO) {
        try {
            val driveClient = getDriveClient()
                ?: return@withContext Result.failure(IllegalStateException("User is not signed in to Google"))

            val fileList = driveClient.files().list()
                .setQ("mimeType='application/vnd.google-apps.spreadsheet' and trashed=false")
                .setFields("files(id, name, webViewLink)")
                .execute()

            val list = fileList.files?.map { file ->
                SpreadsheetInfo(
                    id = file.id,
                    name = file.name,
                    url = file.webViewLink ?: ""
                )
            } ?: emptyList()

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyAndSetupHeaders(spreadsheetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sheetsClient = getSheetsClient()
                ?: return@withContext Result.failure(IllegalStateException("User is not signed in to Google"))

            // Check if headers already exist
            val response = sheetsClient.spreadsheets().values()
                .get(spreadsheetId, AppConstants.SHEET_HEADERS_RANGE)
                .execute()

            val existingValues = response.getValues()
            if (existingValues.isNullOrEmpty() || existingValues.first().isEmpty()) {
                val headerBody = ValueRange().setValues(listOf(GoogleSheetsMapper.SHEET_HEADERS))
                sheetsClient.spreadsheets().values()
                    .update(spreadsheetId, AppConstants.SHEET_HEADERS_RANGE, headerBody)
                    .setValueInputOption("USER_ENTERED")
                    .execute()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun appendTransactions(
        spreadsheetId: String,
        transactions: List<Transaction>
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) return@withContext Result.success(0)

        try {
            val sheetsClient = getSheetsClient()
                ?: return@withContext Result.failure(IllegalStateException("User is not signed in to Google"))

            verifyAndSetupHeaders(spreadsheetId)

            // Batch in chunks of max 100 rows per request (PRD Section 9.3)
            val chunks = transactions.chunked(AppConstants.MAX_BATCH_SYNC_ROWS)
            var totalPushed = 0

            for (chunk in chunks) {
                val rows = chunk.map { GoogleSheetsMapper.toSheetRow(it) }
                val body = ValueRange().setValues(rows)

                sheetsClient.spreadsheets().values()
                    .append(spreadsheetId, "Transactions!A2", body)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()

                totalPushed += chunk.size
            }

            Result.success(totalPushed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readAllTransactions(spreadsheetId: String): Result<List<Transaction>> = withContext(Dispatchers.IO) {
        try {
            val sheetsClient = getSheetsClient()
                ?: return@withContext Result.failure(IllegalStateException("User is not signed in to Google"))

            val response = sheetsClient.spreadsheets().values()
                .get(spreadsheetId, AppConstants.SHEET_RANGE_ALL)
                .execute()

            val values = response.getValues() ?: emptyList<List<Any?>>()
            val parsedList = values.mapNotNull { row ->
                GoogleSheetsMapper.fromSheetRow(row)
            }

            Result.success(parsedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
