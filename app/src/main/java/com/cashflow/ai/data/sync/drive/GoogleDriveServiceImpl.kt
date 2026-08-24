package com.cashflow.ai.data.sync.drive

import android.content.Context
import com.cashflow.ai.data.sync.auth.GoogleAuthManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GoogleDriveServiceImpl(
    private val context: Context,
    private val authManager: GoogleAuthManager
) : GoogleDriveService {

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()
    private var cachedFolderId: String? = null

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

    override suspend fun uploadReceiptImage(imageFile: File): Result<String> = withContext(Dispatchers.IO) {
        if (!imageFile.exists()) {
            return@withContext Result.failure(IllegalArgumentException("Image file does not exist: ${imageFile.absolutePath}"))
        }

        try {
            val driveClient = getDriveClient()
                ?: return@withContext Result.failure(IllegalStateException("User is not signed in to Google"))

            val folderId = getOrCreateReceiptsFolder(driveClient)

            val fileMetadata = DriveFile().apply {
                name = imageFile.name
                parents = listOf(folderId)
            }

            val mediaContent = FileContent("image/jpeg", imageFile)
            val uploadedFile = driveClient.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink, webContentLink")
                .execute()

            val link = uploadedFile.webViewLink ?: uploadedFile.webContentLink ?: "https://drive.google.com/file/d/${uploadedFile.id}/view"
            Result.success(link)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOrCreateReceiptsFolder(drive: Drive): String {
        cachedFolderId?.let { return it }

        // Search for folder
        val query = "mimeType='application/vnd.google-apps.folder' and name='CashFlow Receipts' and trashed=false"
        val result = drive.files().list()
            .setQ(query)
            .setFields("files(id, name)")
            .execute()

        val existing = result.files?.firstOrNull()
        if (existing != null) {
            cachedFolderId = existing.id
            return existing.id
        }

        // Create folder
        val folderMetadata = DriveFile().apply {
            name = "CashFlow Receipts"
            mimeType = "application/vnd.google-apps.folder"
        }

        val createdFolder = drive.files().create(folderMetadata)
            .setFields("id")
            .execute()

        cachedFolderId = createdFolder.id
        return createdFolder.id
    }
}
