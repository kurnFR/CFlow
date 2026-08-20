package com.cashflow.ai.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashflow.ai.core.util.ImagePreprocessor
import com.cashflow.ai.domain.model.ReceiptData
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.ai.AiScanState
import com.cashflow.ai.domain.usecase.ai.ProcessReceiptUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CameraScanUiState(
    val scanState: AiScanState = AiScanState.Idle,
    val isTorchOn: Boolean = false,
    val isFrontCamera: Boolean = false,
    val scannedReceiptData: ReceiptData? = null,
    val draftTransaction: Transaction? = null
)

class CameraScanViewModel(
    private val processReceiptUseCase: ProcessReceiptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraScanUiState())
    val uiState: StateFlow<CameraScanUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(context: Context, file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(scanState = AiScanState.Preprocessing) }

            val bitmap = ImagePreprocessor.loadAndOptimizeBitmap(file)
            if (bitmap == null) {
                _uiState.update {
                    it.copy(scanState = AiScanState.Error("Failed to decode captured photo"))
                }
                return@launch
            }

            processReceiptImage(bitmap, file.absolutePath)
        }
    }

    fun onGalleryImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(scanState = AiScanState.Preprocessing) }

            val bitmap = ImagePreprocessor.loadAndOptimizeBitmap(context, uri)
            if (bitmap == null) {
                _uiState.update {
                    it.copy(scanState = AiScanState.Error("Failed to load image from gallery"))
                }
                return@launch
            }

            val cachedFile = ImagePreprocessor.saveBitmapToCache(context, bitmap)
            processReceiptImage(bitmap, cachedFile.absolutePath)
        }
    }

    private suspend fun processReceiptImage(bitmap: Bitmap, imagePath: String) {
        processReceiptUseCase(bitmap, imagePath).collect { state ->
            _uiState.update { current ->
                when (state) {
                    is AiScanState.Success -> current.copy(
                        scanState = state,
                        scannedReceiptData = state.receiptData,
                        draftTransaction = state.draftTransaction
                    )
                    else -> current.copy(scanState = state)
                }
            }
        }
    }

    fun resetState() {
        _uiState.update {
            CameraScanUiState(scanState = AiScanState.Idle)
        }
    }

    class Factory(
        private val processReceiptUseCase: ProcessReceiptUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CameraScanViewModel(processReceiptUseCase) as T
        }
    }
}
