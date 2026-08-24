package com.cashflow.ai.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.cashflow.ai.domain.model.ai.OcrResult

interface OcrEngine {
    suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult>
    suspend fun recognizeTextFromUri(context: Context, uri: Uri): Result<OcrResult>
}
