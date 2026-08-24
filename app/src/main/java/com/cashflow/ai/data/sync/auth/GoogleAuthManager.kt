package com.cashflow.ai.data.sync.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cashflow.ai.core.constants.AppConstants
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.sync.GoogleAccountInfo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GoogleAuthManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                AppConstants.PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard SharedPreferences if Keystore error occurs
            context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _accountState = MutableStateFlow<GoogleAccountInfo?>(null)
    val accountState: StateFlow<GoogleAccountInfo?> = _accountState.asStateFlow()

    init {
        loadPersistedAccount()
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(SheetsScopes.SPREADSHEETS),
                Scope(DriveScopes.DRIVE_FILE)
            )
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return getGoogleSignInClient().signInIntent
    }

    fun handleSignInResult(account: GoogleSignInAccount?): Boolean {
        if (account == null || account.email.isNullOrBlank()) {
            return false
        }

        val email = account.email!!
        val displayName = account.displayName
        val photoUrl = account.photoUrl?.toString()

        sharedPreferences.edit()
            .putString(KEY_ACCOUNT_EMAIL, email)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_PHOTO_URL, photoUrl)
            .apply()

        _accountState.update {
            GoogleAccountInfo(
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                spreadsheetId = getSpreadsheetId(),
                spreadsheetName = getSpreadsheetName(),
                isConnected = true
            )
        }
        return true
    }

    fun signOut(onComplete: () -> Unit = {}) {
        getGoogleSignInClient().signOut().addOnCompleteListener {
            sharedPreferences.edit()
                .remove(KEY_ACCOUNT_EMAIL)
                .remove(KEY_DISPLAY_NAME)
                .remove(KEY_PHOTO_URL)
                .remove(KEY_SPREADSHEET_ID)
                .remove(KEY_SPREADSHEET_NAME)
                .apply()

            _accountState.value = null
            onComplete()
        }
    }

    fun getSpreadsheetId(): String? {
        return sharedPreferences.getString(KEY_SPREADSHEET_ID, null)
    }

    fun setSpreadsheet(id: String, name: String) {
        sharedPreferences.edit()
            .putString(KEY_SPREADSHEET_ID, id)
            .putString(KEY_SPREADSHEET_NAME, name)
            .apply()

        _accountState.update { current ->
            current?.copy(
                spreadsheetId = id,
                spreadsheetName = name
            )
        }
    }

    fun getSpreadsheetName(): String {
        return sharedPreferences.getString(KEY_SPREADSHEET_NAME, AppConstants.DEFAULT_SHEET_NAME)
            ?: AppConstants.DEFAULT_SHEET_NAME
    }

    fun getLastSyncTimestamp(): Long {
        return sharedPreferences.getLong(KEY_LAST_SYNC, 0L)
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        sharedPreferences.edit()
            .putLong(KEY_LAST_SYNC, timestamp)
            .apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_AUTO_SYNC, enabled)
            .apply()
    }

    fun isDrivePhotoSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DRIVE_PHOTO_SYNC, true)
    }

    fun setDrivePhotoSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_DRIVE_PHOTO_SYNC, enabled)
            .apply()
    }

    fun getDefaultCurrency(): Currency {
        val curStr = sharedPreferences.getString(KEY_DEFAULT_CURRENCY, "IDR") ?: "IDR"
        return if (curStr.equals("USD", ignoreCase = true)) Currency.USD else Currency.IDR
    }

    fun setDefaultCurrency(currency: Currency) {
        sharedPreferences.edit()
            .putString(KEY_DEFAULT_CURRENCY, currency.name)
            .apply()
    }

    fun isAiEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AI_ENABLED, true)
    }

    fun setAiEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_AI_ENABLED, enabled)
            .apply()
    }

    fun isCloudAiEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_CLOUD_AI_ENABLED, true)
    }

    fun setCloudAiEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_CLOUD_AI_ENABLED, enabled)
            .apply()
    }

    private fun loadPersistedAccount() {
        val email = sharedPreferences.getString(KEY_ACCOUNT_EMAIL, null)
        if (!email.isNullOrBlank()) {
            val displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, null)
            val photoUrl = sharedPreferences.getString(KEY_PHOTO_URL, null)
            val sheetId = getSpreadsheetId()
            val sheetName = getSpreadsheetName()

            _accountState.value = GoogleAccountInfo(
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                spreadsheetId = sheetId,
                spreadsheetName = sheetName,
                isConnected = true
            )
        }
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    companion object {
        private const val KEY_ACCOUNT_EMAIL = "key_account_email"
        private const val KEY_DISPLAY_NAME = "key_display_name"
        private const val KEY_PHOTO_URL = "key_photo_url"
        private const val KEY_SPREADSHEET_ID = "key_spreadsheet_id"
        private const val KEY_SPREADSHEET_NAME = "key_spreadsheet_name"
        private const val KEY_LAST_SYNC = "key_last_sync_timestamp"
        private const val KEY_AUTO_SYNC = "key_auto_sync_enabled"
        private const val KEY_DRIVE_PHOTO_SYNC = "key_drive_photo_sync_enabled"
        private const val KEY_DEFAULT_CURRENCY = "key_default_currency"
        private const val KEY_AI_ENABLED = "key_ai_enabled"
        private const val KEY_CLOUD_AI_ENABLED = "key_cloud_ai_enabled"
    }
}
