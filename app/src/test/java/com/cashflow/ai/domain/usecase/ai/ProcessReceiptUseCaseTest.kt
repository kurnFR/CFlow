package com.cashflow.ai.domain.usecase.ai

import android.graphics.Bitmap
import com.cashflow.ai.domain.ai.CategoryClassifier
import com.cashflow.ai.domain.ai.OcrEngine
import com.cashflow.ai.domain.ai.ReceiptParser
import com.cashflow.ai.domain.model.CategorySource
import com.cashflow.ai.domain.model.CategorySuggestion
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.ReceiptConfidence
import com.cashflow.ai.domain.model.ReceiptData
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.domain.model.ai.AiScanState
import com.cashflow.ai.domain.model.ai.OcrResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ProcessReceiptUseCaseTest {

    private lateinit var mockOcrEngine: OcrEngine
    private lateinit var mockReceiptParser: ReceiptParser
    private lateinit var mockCategoryClassifier: CategoryClassifier
    private lateinit var processReceiptUseCase: ProcessReceiptUseCase

    @Before
    fun setUp() {
        mockOcrEngine = mock(OcrEngine::class.java)
        mockReceiptParser = mock(ReceiptParser::class.java)
        mockCategoryClassifier = mock(CategoryClassifier::class.java)

        processReceiptUseCase = ProcessReceiptUseCase(
            ocrEngine = mockOcrEngine,
            receiptParser = mockReceiptParser,
            categoryClassifier = mockCategoryClassifier
        )
    }

    @Test
    fun invoke_successfulPipeline_emitsExpectedStatesAndDraftTransaction() = runBlocking {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(800)
        `when`(mockBitmap.height).thenReturn(1200)

        val rawText = "INDOMARET\nTotal: Rp 50.000\nDate: 2026-08-20"
        val ocrResult = OcrResult(fullText = rawText, confidence = 0.95f)

        val parsedReceipt = ReceiptData(
            merchant = "INDOMARET",
            total = 50000.0,
            currency = Currency.IDR,
            date = "2026-08-20",
            confidence = ReceiptConfidence(0.9, 0.9, 0.9, 0.9),
            rawText = rawText
        )

        val categorySuggestion = CategorySuggestion(
            category = "Groceries",
            confidence = 0.95,
            source = CategorySource.RULE
        )

        // Mock behaviors
        `when`(mockOcrEngine.recognizeText(org.mockito.ArgumentMatchers.any())).thenReturn(Result.success(ocrResult))
        `when`(mockReceiptParser.parseReceipt(org.mockito.ArgumentMatchers.eq(rawText), org.mockito.ArgumentMatchers.any())).thenReturn(Result.success(parsedReceipt))
        `when`(mockCategoryClassifier.classify(
            description = org.mockito.ArgumentMatchers.anyString(),
            merchant = org.mockito.ArgumentMatchers.anyString(),
            amount = org.mockito.ArgumentMatchers.anyDouble(),
            type = org.mockito.ArgumentMatchers.any()
        )).thenReturn(categorySuggestion)

        val states = processReceiptUseCase(
            bitmap = mockBitmap,
            forcedType = TransactionType.EXPENSE
        ).toList()

        assertTrue(states.isNotEmpty())
        assertTrue(states[0] is AiScanState.Preprocessing)
        assertTrue(states[1] is AiScanState.ExtractingText)
        assertTrue(states[2] is AiScanState.ParsingReceipt)
        assertTrue(states[3] is AiScanState.ClassifyingCategory)
        assertTrue(states[4] is AiScanState.Success)

        val successState = states[4] as AiScanState.Success
        assertEquals("INDOMARET", successState.receiptData.merchant)
        assertEquals(50000.0, successState.receiptData.total ?: 0.0, 0.01)
        assertEquals("Groceries", successState.receiptData.suggestedCategory)

        val draft = successState.draftTransaction
        assertEquals("INDOMARET", draft.description)
        assertEquals(50000.0, draft.amount, 0.01)
        assertEquals("Groceries", draft.category)
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals(TransactionSource.PHOTO, draft.source)
    }

    @Test
    fun invoke_ocrFailure_emitsErrorState() = runBlocking {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(800)
        `when`(mockBitmap.height).thenReturn(1200)

        `when`(mockOcrEngine.recognizeText(org.mockito.ArgumentMatchers.any())).thenReturn(
            Result.failure(RuntimeException("Camera blur error"))
        )

        val states = processReceiptUseCase(mockBitmap).toList()

        assertTrue(states.any { it is AiScanState.Error })
        val error = states.last() as AiScanState.Error
        assertEquals("Camera blur error", error.message)
    }
}
