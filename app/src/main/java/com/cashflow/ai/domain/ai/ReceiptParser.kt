package com.cashflow.ai.domain.ai

import android.graphics.Bitmap
import com.cashflow.ai.domain.model.ReceiptData

interface ReceiptParser {
    suspend fun parseReceipt(rawText: String, bitmap: Bitmap? = null): Result<ReceiptData>
}
