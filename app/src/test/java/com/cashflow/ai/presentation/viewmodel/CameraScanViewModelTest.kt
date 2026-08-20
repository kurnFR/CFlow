package com.cashflow.ai.presentation.viewmodel

import com.cashflow.ai.domain.model.ReceiptConfidence
import com.cashflow.ai.domain.model.ReceiptData
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.ai.AiScanState
import com.cashflow.ai.domain.usecase.ai.ProcessReceiptUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class CameraScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockProcessReceiptUseCase: ProcessReceiptUseCase
    private lateinit var viewModel: CameraScanViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockProcessReceiptUseCase = mock(ProcessReceiptUseCase::class.java)
        viewModel = CameraScanViewModel(mockProcessReceiptUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() {
        assertTrue(viewModel.uiState.value.scanState is AiScanState.Idle)
    }

    @Test
    fun resetState_resetsScanStateToIdle() {
        viewModel.resetState()
        assertTrue(viewModel.uiState.value.scanState is AiScanState.Idle)
    }
}
