package com.cashflow.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.data.sync.SyncManager
import com.cashflow.ai.data.sync.auth.GoogleAuthManager
import com.cashflow.ai.data.sync.sheets.GoogleSheetsService
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.SyncStatus
import com.cashflow.ai.domain.model.sync.GoogleAccountInfo
import com.cashflow.ai.domain.model.sync.SpreadsheetInfo
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUiState(
    val accountInfo: GoogleAccountInfo? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncFormatted: String = "Never synced",
    val availableSpreadsheets: List<SpreadsheetInfo> = emptyList(),
    val isAutoSyncEnabled: Boolean = true,
    val isDrivePhotoSyncEnabled: Boolean = true,
    val defaultCurrency: Currency = Currency.IDR,
    val isAiEnabled: Boolean = true,
    val isCloudAiEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val isLoadingSheets: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val authManager: GoogleAuthManager,
    private val syncManager: SyncManager,
    private val sheetsService: GoogleSheetsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
        observeSyncStatus()
        loadPreferences()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.accountState.collect { account ->
                _uiState.update { it.copy(accountInfo = account) }
                if (account != null) {
                    loadAvailableSpreadsheets()
                }
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncManager.syncStatus.collect { status ->
                _uiState.update {
                    it.copy(
                        syncStatus = status,
                        isSyncing = status == SyncStatus.SYNCING,
                        lastSyncFormatted = formatLastSync(authManager.getLastSyncTimestamp())
                    )
                }
            }
        }
    }

    private fun loadPreferences() {
        _uiState.update {
            it.copy(
                isAutoSyncEnabled = authManager.isAutoSyncEnabled(),
                isDrivePhotoSyncEnabled = authManager.isDrivePhotoSyncEnabled(),
                defaultCurrency = authManager.getDefaultCurrency(),
                isAiEnabled = authManager.isAiEnabled(),
                isCloudAiEnabled = authManager.isCloudAiEnabled(),
                lastSyncFormatted = formatLastSync(authManager.getLastSyncTimestamp())
            )
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val result = syncManager.sync()
            if (result.isSuccess) {
                val syncResult = result.getOrNull()
                val pushed = syncResult?.pushedCount ?: 0
                val pulled = syncResult?.pulledCount ?: 0
                val uploaded = syncResult?.imagesUploadedCount ?: 0
                _uiState.update {
                    it.copy(
                        statusMessage = "Synced: +$pushed uploaded, $pulled pulled, $uploaded images backed up.",
                        lastSyncFormatted = formatLastSync(authManager.getLastSyncTimestamp())
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        statusMessage = "Sync failed: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun handleSignInResult(account: GoogleSignInAccount?) {
        val success = authManager.handleSignInResult(account)
        if (success) {
            _uiState.update { it.copy(statusMessage = "Signed in successfully!") }
            loadAvailableSpreadsheets()
        } else {
            _uiState.update { it.copy(statusMessage = "Google Sign-In failed.") }
        }
    }

    fun signOut() {
        authManager.signOut {
            _uiState.update {
                it.copy(
                    accountInfo = null,
                    availableSpreadsheets = emptyList(),
                    statusMessage = "Signed out"
                )
            }
        }
    }

    fun loadAvailableSpreadsheets() {
        _uiState.update { it.copy(isLoadingSheets = true) }
        viewModelScope.launch {
            val result = sheetsService.listSpreadsheets()
            _uiState.update {
                it.copy(
                    isLoadingSheets = false,
                    availableSpreadsheets = result.getOrDefault(emptyList())
                )
            }
        }
    }

    fun createNewSpreadsheet(title: String) {
        viewModelScope.launch {
            val result = sheetsService.createSpreadsheet(title)
            if (result.isSuccess) {
                val info = result.getOrNull()
                _uiState.update {
                    it.copy(statusMessage = "Created spreadsheet: ${info?.name}")
                }
                loadAvailableSpreadsheets()
            } else {
                _uiState.update {
                    it.copy(statusMessage = "Failed to create spreadsheet: ${result.exceptionOrNull()?.localizedMessage}")
                }
            }
        }
    }

    fun selectSpreadsheet(info: SpreadsheetInfo) {
        authManager.setSpreadsheet(info.id, info.name)
        _uiState.update { it.copy(statusMessage = "Selected spreadsheet: ${info.name}") }
    }

    fun toggleAutoSync(enabled: Boolean) {
        authManager.setAutoSyncEnabled(enabled)
        _uiState.update { it.copy(isAutoSyncEnabled = enabled) }
    }

    fun toggleDrivePhotoSync(enabled: Boolean) {
        authManager.setDrivePhotoSyncEnabled(enabled)
        _uiState.update { it.copy(isDrivePhotoSyncEnabled = enabled) }
    }

    fun setDefaultCurrency(currency: Currency) {
        authManager.setDefaultCurrency(currency)
        _uiState.update { it.copy(defaultCurrency = currency) }
    }

    fun toggleAiEnabled(enabled: Boolean) {
        authManager.setAiEnabled(enabled)
        _uiState.update { it.copy(isAiEnabled = enabled) }
    }

    fun toggleCloudAiEnabled(enabled: Boolean) {
        authManager.setCloudAiEnabled(enabled)
        _uiState.update { it.copy(isCloudAiEnabled = enabled) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun formatLastSync(timestamp: Long): String {
        if (timestamp <= 0) return "Never synced"
        val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }

    class Factory(
        private val authManager: GoogleAuthManager,
        private val syncManager: SyncManager,
        private val sheetsService: GoogleSheetsService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(authManager, syncManager, sheetsService) as T
        }
    }
}
