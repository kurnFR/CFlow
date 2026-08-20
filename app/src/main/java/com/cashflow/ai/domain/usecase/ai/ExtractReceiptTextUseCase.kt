package com.cashflow.ai.domain.usecase.ai

import android.graphics.Bitmap
import com.cashflow.ai.core.util.ImagePreprocessor
import com.cashflow.ai.domain.ai.OcrEngine
import com.cashflow.ai.domain.model.ai.OcrResult

class ExtractReceiptTextUseCase(
    private val ocrEngine: OcrEngine
) {
    suspend operator fun invoke(bitmap: Bitmap, enhanceForOcr: Boolean = true): Result<OcrResult> {
        val processed = if (enhanceForOcr) {
            ImagePreprocessor.enhanceForOcr(bitmap)
        } else {
            bitmap
        }
        return ocrEngine.recognizeText(processed)
    }
}
