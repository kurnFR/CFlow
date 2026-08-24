package com.cashflow.ai.data.sync

import com.cashflow.ai.data.sync.auth.GoogleAuthManager
import com.cashflow.ai.data.sync.drive.GoogleDriveService
import com.cashflow.ai.data.sync.sheets.GoogleSheetsService
import com.cashflow.ai.domain.model.SyncStatus
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.sync.ConflictResolution
import com.cashflow.ai.domain.model.sync.SyncResult
import com.cashflow.ai.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

class SyncManager(
    private val transactionRepository: TransactionRepository,
    private val sheetsService: GoogleSheetsService,
    private val driveService: GoogleDriveService,
    private val authManager: GoogleAuthManager
) {

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    suspend fun sync(): Result<SyncResult> = withContext(Dispatchers.IO) {
        val spreadsheetId = authManager.getSpreadsheetId()
        if (spreadsheetId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No Google Spreadsheet configured. Please connect a spreadsheet in Settings."))
        }

        _syncStatus.value = SyncStatus.SYNCING

        try {
            var pushedCount = 0
            var pulledCount = 0
            var imagesUploadedCount = 0
            var conflictsResolvedCount = 0

            // 1. Upload Local Receipt Photos to Drive (if enabled)
            val unsyncedTransactions = transactionRepository.getUnsyncedTransactions()
            val preparedTransactions = if (authManager.isDrivePhotoSyncEnabled()) {
                unsyncedTransactions.map { tx ->
                    if (!tx.imageUrl.isNullOrBlank() && !tx.imageUrl.startsWith("http")) {
                        val file = File(tx.imageUrl)
                        if (file.exists()) {
                            val uploadResult = driveService.uploadReceiptImage(file)
                            if (uploadResult.isSuccess) {
                                imagesUploadedCount++
                                tx.copy(imageUrl = uploadResult.getOrNull())
                            } else {
                                tx
                            }
                        } else {
                            tx
                        }
                    } else {
                        tx
                    }
                }
            } else {
                unsyncedTransactions
            }

            // 2. Push unsynced transactions to Google Sheets
            if (preparedTransactions.isNotEmpty()) {
                val appendResult = sheetsService.appendTransactions(spreadsheetId, preparedTransactions)
                if (appendResult.isFailure) {
                    _syncStatus.value = SyncStatus.ERROR
                    return@withContext Result.failure(appendResult.exceptionOrNull() ?: Exception("Failed to append rows to Sheets"))
                }
                pushedCount = appendResult.getOrDefault(0)

                // Mark local transactions as synced
                val idsToMark = preparedTransactions.map { it.id }.filter { it > 0 }
                if (idsToMark.isNotEmpty()) {
                    transactionRepository.markAsSynced(idsToMark, newVersion = 1)
                }
            }

            // 3. Pull remote transactions from Google Sheets
            val readResult = sheetsService.readAllTransactions(spreadsheetId)
            if (readResult.isSuccess) {
                val remoteTransactions = readResult.getOrDefault(emptyList())
                if (remoteTransactions.isNotEmpty()) {
                    val localAll = transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()
                    val localUuidMap = localAll.associateBy { it.uuid }

                    val toUpsert = mutableListOf<Transaction>()
                    for (remote in remoteTransactions) {
                        val local = localUuidMap[remote.uuid]
                        if (local == null) {
                            // New transaction from Sheets -> insert locally
                            toUpsert.add(remote)
                            pulledCount++
                        } else {
                            // Existing transaction -> check conflict
                            val resolution = ConflictResolver.resolve(local, remote)
                            if (resolution == ConflictResolution.REMOTE_WINS) {
                                toUpsert.add(remote.copy(id = local.id))
                                conflictsResolvedCount++
                            }
                        }
                    }

                    if (toUpsert.isNotEmpty()) {
                        transactionRepository.upsertRemoteTransactions(toUpsert)
                    }
                }
            }

            // 4. Update sync timestamp and status
            val syncTimestamp = System.currentTimeMillis()
            authManager.setLastSyncTimestamp(syncTimestamp)
            _syncStatus.value = SyncStatus.SUCCESS

            val result = SyncResult(
                isSuccess = true,
                pushedCount = pushedCount,
                pulledCount = pulledCount,
                imagesUploadedCount = imagesUploadedCount,
                conflictsResolvedCount = conflictsResolvedCount,
                timestamp = syncTimestamp
            )
            Result.success(result)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }
}
