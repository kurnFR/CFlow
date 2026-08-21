package com.cashflow.ai.data.sync.drive

import java.io.File

interface GoogleDriveService {
    suspend fun uploadReceiptImage(imageFile: File): Result<String>
}
