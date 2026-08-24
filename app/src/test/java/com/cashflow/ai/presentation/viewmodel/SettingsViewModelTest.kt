package com.cashflow.ai.presentation.viewmodel

import com.cashflow.ai.data.sync.SyncManager
import com.cashflow.ai.data.sync.auth.GoogleAuthManager
import com.cashflow.ai.data.sync.sheets.GoogleSheetsService
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.SyncStatus
import com.cashflow.ai.domain.model.sync.GoogleAccountInfo
import com.cashflow.ai.domain.model.sync.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockAuthManager: GoogleAuthManager
    private lateinit var mockSyncManager: SyncManager
    private lateinit var mockSheetsService: GoogleSheetsService
    private lateinit var viewModel: SettingsViewModel

    private val authStateFlow = MutableStateFlow<GoogleAccountInfo?>(null)
    private val syncStatusFlow = MutableStateFlow(SyncStatus.IDLE)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockAuthManager = mock(GoogleAuthManager::class.java)
        mockSyncManager = mock(SyncManager::class.java)
        mockSheetsService = mock(GoogleSheetsService::class.java)

        `when`(mockAuthManager.accountState).thenReturn(authStateFlow)
        `when`(mockSyncManager.syncStatus).thenReturn(syncStatusFlow)
        `when`(mockAuthManager.isAutoSyncEnabled()).thenReturn(true)
        `when`(mockAuthManager.isDrivePhotoSyncEnabled()).thenReturn(true)
        `when`(mockAuthManager.getDefaultCurrency()).thenReturn(Currency.IDR)
        `when`(mockAuthManager.isAiEnabled()).thenReturn(true)
        `when`(mockAuthManager.isCloudAiEnabled()).thenReturn(true)
        `when`(mockAuthManager.getLastSyncTimestamp()).thenReturn(0L)

        viewModel = SettingsViewModel(mockAuthManager, mockSyncManager, mockSheetsService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleAutoSync_callsAuthManagerAndUpdatesState() = runTest {
        viewModel.toggleAutoSync(false)
        advanceUntilIdle()

        verify(mockAuthManager).setAutoSyncEnabled(false)
        assertEquals(false, viewModel.uiState.value.isAutoSyncEnabled)
    }

    @Test
    fun setDefaultCurrency_callsAuthManagerAndUpdatesState() = runTest {
        viewModel.setDefaultCurrency(Currency.USD)
        advanceUntilIdle()

        verify(mockAuthManager).setDefaultCurrency(Currency.USD)
        assertEquals(Currency.USD, viewModel.uiState.value.defaultCurrency)
    }

    @Test
    fun triggerManualSync_successfulSync_updatesStatusMessage() = runTest {
        val syncResult = SyncResult(isSuccess = true, pushedCount = 3, pulledCount = 2, imagesUploadedCount = 1)
        `when`(mockSyncManager.sync()).thenReturn(Result.success(syncResult))

        viewModel.triggerManualSync()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.statusMessage)
        assertTrue(viewModel.uiState.value.statusMessage?.contains("+3 uploaded") == true)
    }
}
