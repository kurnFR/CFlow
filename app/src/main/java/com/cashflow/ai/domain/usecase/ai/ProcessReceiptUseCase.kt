package com.cashflow.ai.domain.usecase.ai

import android.graphics.Bitmap
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.core.util.ImagePreprocessor
import com.cashflow.ai.domain.ai.CategoryClassifier
import com.cashflow.ai.domain.ai.OcrEngine
import com.cashflow.ai.domain.ai.ReceiptParser
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.model.ai.AiScanState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

class ProcessReceiptUseCase(
    private val ocrEngine: OcrEngine,
    private val receiptParser: ReceiptParser,
    private val categoryClassifier: CategoryClassifier
) {

    operator fun invoke(
        bitmap: Bitmap,
        imagePath: String? = null,
        forcedType: TransactionType = TransactionType.EXPENSE
    ): Flow<AiScanState> = flow {
        emit(AiScanState.Preprocessing)

        // 1. Preprocessing (enhancing contrast & grayscale for OCR)
        val enhancedBitmap = ImagePreprocessor.enhanceForOcr(bitmap)

        emit(AiScanState.ExtractingText(0.4f))

        // 2. OCR Text Extraction
        val ocrResult = ocrEngine.recognizeText(enhancedBitmap)
        if (ocrResult.isFailure) {
            val errorMsg = ocrResult.exceptionOrNull()?.localizedMessage ?: "OCR text extraction failed"
            emit(AiScanState.Error(message = errorMsg, cause = ocrResult.exceptionOrNull()))
            return@flow
        }

        val rawText = ocrResult.getOrNull()?.fullText ?: ""
        if (rawText.isBlank()) {
            emit(AiScanState.Error(message = "No readable text found on the receipt image"))
            return@flow
        }

        emit(AiScanState.ParsingReceipt(isCloud = true))

        // 3. Receipt Information Extraction (Gemini with Local Regex Fallback)
        val parseResult = receiptParser.parseReceipt(rawText, bitmap)
        if (parseResult.isFailure) {
            val errorMsg = parseResult.exceptionOrNull()?.localizedMessage ?: "Receipt parsing failed"
            emit(AiScanState.Error(message = errorMsg, rawText = rawText, cause = parseResult.exceptionOrNull()))
            return@flow
        }

        val receiptData = parseResult.getOrNull()
        if (receiptData == null) {
            emit(AiScanState.Error(message = "Could not parse receipt fields", rawText = rawText))
            return@flow
        }

        emit(AiScanState.ClassifyingCategory)

        // 4. Smart Category Classification
        val description = receiptData.merchant ?: receiptData.items.firstOrNull() ?: "Receipt Expense"
        val categorySuggestion = categoryClassifier.classify(
            description = description,
            merchant = receiptData.merchant,
            amount = receiptData.total,
            type = forcedType
        )

        val finalReceiptData = receiptData.copy(
            suggestedCategory = categorySuggestion.category,
            categoryConfidence = categorySuggestion.confidence
        )

        // 5. Construct draft Transaction
        val draftTransaction = Transaction(
            id = 0,
            uuid = UUID.randomUUID().toString(),
            date = finalReceiptData.date ?: DateUtils.getCurrentDateString(),
            description = finalReceiptData.merchant ?: (if (finalReceiptData.items.isNotEmpty()) finalReceiptData.items.joinToString(", ") else "Receipt Purchase"),
            amount = finalReceiptData.total ?: 0.0,
            category = categorySuggestion.category,
            type = forcedType,
            source = TransactionSource.PHOTO,
            imageUrl = imagePath,
            currency = finalReceiptData.currency,
            isSynced = false,
            syncVersion = 1,
            aiConfidence = finalReceiptData.confidence.overall,
            aiMerchant = finalReceiptData.merchant,
            tax = finalReceiptData.tax,
            discount = finalReceiptData.discount,
            itemsSummary = if (finalReceiptData.items.isNotEmpty()) finalReceiptData.items.take(3).joinToString("; ") else null
        )

        emit(AiScanState.Success(finalReceiptData, draftTransaction))
    }.flowOn(Dispatchers.Default)
}
