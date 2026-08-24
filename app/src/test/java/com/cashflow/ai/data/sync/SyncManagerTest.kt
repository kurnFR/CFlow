package com.cashflow.ai.data.sync

import com.cashflow.ai.data.sync.auth.GoogleAuthManager
import com.cashflow.ai.data.sync.drive.GoogleDriveService
import com.cashflow.ai.data.sync.sheets.GoogleSheetsService
import com.cashflow.ai.domain.model.SyncStatus
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class SyncManagerTest {

    private lateinit var mockRepository: TransactionRepository
    private lateinit var mockSheetsService: GoogleSheetsService
    private lateinit var mockDriveService: GoogleDriveService
    private lateinit var mockAuthManager: GoogleAuthManager
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        mockRepository = mock(TransactionRepository::class.java)
        mockSheetsService = mock(GoogleSheetsService::class.java)
        mockDriveService = mock(GoogleDriveService::class.java)
        mockAuthManager = mock(GoogleAuthManager::class.java)

        `when`(mockAuthManager.getSpreadsheetId()).thenReturn("sheet-test-id-123")
        `when`(mockAuthManager.isDrivePhotoSyncEnabled()).thenReturn(false)

        syncManager = SyncManager(
            transactionRepository = mockRepository,
            sheetsService = mockSheetsService,
            driveService = mockDriveService,
            authManager = mockAuthManager
        )
    }

    @Test
    fun sync_pushesUnsyncedAndPullsRemote_success() = runBlocking {
        val unsyncedLocal = listOf(
            Transaction(id = 1, uuid = "u1", description = "Coffee", amount = 30000.0, category = "Food & Dining", date = "2026-08-21")
        )
        val remoteList = listOf(
            Transaction(id = 0, uuid = "u2", description = "Salary", amount = 10000000.0, category = "Salary", date = "2026-08-20")
        )

        `when`(mockRepository.getUnsyncedTransactions()).thenReturn(unsyncedLocal)
        `when`(mockSheetsService.appendTransactions(anyString(), anyList())).thenReturn(Result.success(1))
        `when`(mockRepository.markAsSynced(anyList(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(Result.success(Unit))
        `when`(mockSheetsService.readAllTransactions(anyString())).thenReturn(Result.success(remoteList))
        `when`(mockRepository.getAllTransactions()).thenReturn(flowOf(unsyncedLocal))
        `when`(mockRepository.upsertRemoteTransactions(anyList())).thenReturn(Result.success(Unit))

        val result = syncManager.sync()
        assertTrue(result.isSuccess)

        val syncResult = result.getOrNull()
        assertEquals(1, syncResult?.pushedCount)
        assertEquals(1, syncResult?.pulledCount)
        assertEquals(SyncStatus.SUCCESS, syncManager.syncStatus.value)
    }

    @Test
    fun sync_noSpreadsheetId_returnsFailure() = runBlocking {
        `when`(mockAuthManager.getSpreadsheetId()).thenReturn(null)

        val result = syncManager.sync()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No Google Spreadsheet") == true)
    }
}
