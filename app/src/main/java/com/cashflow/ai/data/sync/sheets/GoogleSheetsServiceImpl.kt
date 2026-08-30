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
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Request
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

    /** Returns the title of the first sheet (tab) in the spreadsheet, or "Sheet1" fallback. */
    private fun resolveFirstSheetName(sheetsClient: Sheets, spreadsheetId: String): String {
        return try {
            val meta = sheetsClient.spreadsheets().get(spreadsheetId)
                .setFields("sheets.properties.title,sheets.properties.sheetId")
                .execute()
            meta.sheets?.firstOrNull()?.properties?.title ?: "Sheet1"
        } catch (e: Exception) {
            "Sheet1"
        }
    }

    /** Build a range string using the resolved first sheet name to avoid 'Unable to parse range' errors. */
    private fun rangeOnFirstSheet(sheetsClient: Sheets, spreadsheetId: String, suffix: String): String {
        val sheetName = resolveFirstSheetName(sheetsClient, spreadsheetId)
        return "$sheetName$suffix"
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

            // Rename the default first sheet (usually "Sheet1") to "Transactions"
            // so hardcoded ranges like "Transactions!A2" work correctly.
            try {
                val sheetId = created.sheets?.firstOrNull()?.properties?.sheetId
                if (sheetId != null) {
                    val sheetProps = com.google.api.services.sheets.v4.model.SheetProperties()
                    sheetProps.setSheetId(sheetId)
                    sheetProps.setTitle("Transactions")

                    val updateProps = com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest()
                    updateProps.setProperties(sheetProps)
                    updateProps.setFields("title")

                    val updateSheetTitle = Request()
                    updateSheetTitle.setUpdateSheetProperties(updateProps)

                    val batchRequest = BatchUpdateSpreadsheetRequest()
                    batchRequest.setRequests(listOf(updateSheetTitle))

                    sheetsClient.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute()
                }
            } catch (renameException: Exception) {
                // If rename fails (e.g. sheet already named Transactions), continue anyway
            }

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

            val headersRange = rangeOnFirstSheet(sheetsClient, spreadsheetId, "!A1:M1")

            // Check if headers already exist
            val response = sheetsClient.spreadsheets().values()
                .get(spreadsheetId, headersRange)
                .execute()

            val existingValues = response.getValues()
            if (existingValues.isNullOrEmpty() || existingValues.first().isEmpty()) {
                val headerBody = ValueRange().setValues(listOf(GoogleSheetsMapper.SHEET_HEADERS))
                sheetsClient.spreadsheets().values()
                    .update(spreadsheetId, headersRange, headerBody)
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

            // Resolve the actual first sheet name (may be "Sheet1" in existing spreadsheets)
            val appendRange = rangeOnFirstSheet(sheetsClient, spreadsheetId, "!A2")

            // Batch in chunks of max 100 rows per request (PRD Section 9.3)
            val chunks = transactions.chunked(AppConstants.MAX_BATCH_SYNC_ROWS)
            var totalPushed = 0

            for (chunk in chunks) {
                val rows = chunk.map { GoogleSheetsMapper.toSheetRow(it) }
                val body = ValueRange().setValues(rows)

                sheetsClient.spreadsheets().values()
                    .append(spreadsheetId, appendRange, body)
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

            val rangeAll = rangeOnFirstSheet(sheetsClient, spreadsheetId, "!A2:M")

            val response = sheetsClient.spreadsheets().values()
                .get(spreadsheetId, rangeAll)
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
