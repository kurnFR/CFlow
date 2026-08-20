package com.cashflow.ai.data.ai.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.cashflow.ai.core.util.ImagePreprocessor
import com.cashflow.ai.domain.ai.OcrEngine
import com.cashflow.ai.domain.model.ai.OcrBlock
import com.cashflow.ai.domain.model.ai.OcrLine
import com.cashflow.ai.domain.model.ai.OcrResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MlKitOcrEngine : OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val textResult = processImage(inputImage)

            val blocks = textResult.textBlocks.map { block ->
                OcrBlock(
                    text = block.text,
                    lines = block.lines.map { line ->
                        OcrLine(
                            text = line.text,
                            confidence = line.confidence
                        )
                    }
                )
            }

            val fullText = textResult.text.trim()
            if (fullText.isEmpty()) {
                Result.failure(IllegalStateException("No text detected in the provided image"))
            } else {
                Result.success(
                    OcrResult(
                        fullText = fullText,
                        blocks = blocks,
                        confidence = calculateAverageConfidence(textResult)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recognizeTextFromUri(context: Context, uri: Uri): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            val bitmap = ImagePreprocessor.loadAndOptimizeBitmap(context, uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to decode bitmap from URI: $uri"))

            recognizeText(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processImage(image: InputImage): Text = suspendCancellableCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { text ->
                if (cont.isActive) {
                    cont.resume(text)
                }
            }
            .addOnFailureListener { exception ->
                if (cont.isActive) {
                    cont.cancel(exception)
                }
            }
    }

    private fun calculateAverageConfidence(text: Text): Float {
        var count = 0
        var sum = 0f
        text.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                line.confidence?.let {
                    sum += it
                    count++
                }
            }
        }
        return if (count > 0) sum / count else 1.0f
    }
}
