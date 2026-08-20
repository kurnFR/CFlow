package com.cashflow.ai.domain.model.ai

data class OcrLine(
    val text: String,
    val confidence: Float? = null
)

data class OcrBlock(
    val text: String,
    val lines: List<OcrLine> = emptyList()
)

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock> = emptyList(),
    val confidence: Float = 1.0f
)
