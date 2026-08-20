package com.cashflow.ai.domain.model.ai

import com.cashflow.ai.domain.model.ReceiptData
import com.cashflow.ai.domain.model.Transaction

sealed interface AiScanState {
    data object Idle : AiScanState
    data object Preprocessing : AiScanState
    data class ExtractingText(val progress: Float = 0.5f) : AiScanState
    data class ParsingReceipt(val isCloud: Boolean = true) : AiScanState
    data object ClassifyingCategory : AiScanState
    data class Success(
        val receiptData: ReceiptData,
        val draftTransaction: Transaction
    ) : AiScanState
    data class Error(
        val message: String,
        val rawText: String? = null,
        val cause: Throwable? = null
    ) : AiScanState
}
