package com.cashflow.ai

import android.app.Application
import com.cashflow.ai.data.ai.category.SmartCategoryClassifier
import com.cashflow.ai.data.ai.ocr.MlKitOcrEngine
import com.cashflow.ai.data.ai.parser.GeminiReceiptParser
import com.cashflow.ai.data.ai.parser.LocalReceiptParser
import com.cashflow.ai.data.local.CashFlowDatabase
import com.cashflow.ai.data.repository.TransactionRepositoryImpl
import com.cashflow.ai.domain.ai.CategoryClassifier
import com.cashflow.ai.domain.ai.OcrEngine
import com.cashflow.ai.domain.ai.ReceiptParser
import com.cashflow.ai.domain.repository.TransactionRepository
import com.cashflow.ai.domain.usecase.ai.ExtractReceiptTextUseCase
import com.cashflow.ai.domain.usecase.ai.ProcessReceiptUseCase
import com.cashflow.ai.domain.usecase.ai.SuggestCategoryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.cashflow.ai.data.sync.work.InsightWorker

class CashFlowApp : Application() {

    val database: CashFlowDatabase by lazy {
        CashFlowDatabase.getInstance(this)
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao()
        )
    }

    val ocrEngine: OcrEngine by lazy {
        MlKitOcrEngine()
    }

    val localReceiptParser: ReceiptParser by lazy {
        LocalReceiptParser()
    }

    val geminiReceiptParser: ReceiptParser by lazy {
        GeminiReceiptParser(localFallbackParser = localReceiptParser)
    }

    val categoryClassifier: CategoryClassifier by lazy {
        SmartCategoryClassifier(transactionRepository = transactionRepository)
    }

    val processReceiptUseCase: ProcessReceiptUseCase by lazy {
        ProcessReceiptUseCase(
            ocrEngine = ocrEngine,
            receiptParser = geminiReceiptParser,
            categoryClassifier = categoryClassifier
        )
    }

    val suggestCategoryUseCase: SuggestCategoryUseCase by lazy {
        SuggestCategoryUseCase(categoryClassifier = categoryClassifier)
    }

    val extractReceiptTextUseCase: ExtractReceiptTextUseCase by lazy {
        ExtractReceiptTextUseCase(ocrEngine = ocrEngine)
    }

    val googleAuthManager: com.cashflow.ai.data.sync.auth.GoogleAuthManager by lazy {
        com.cashflow.ai.data.sync.auth.GoogleAuthManager(this)
    }

    val googleSheetsService: com.cashflow.ai.data.sync.sheets.GoogleSheetsService by lazy {
        com.cashflow.ai.data.sync.sheets.GoogleSheetsServiceImpl(this, googleAuthManager)
    }

    val googleDriveService: com.cashflow.ai.data.sync.drive.GoogleDriveService by lazy {
        com.cashflow.ai.data.sync.drive.GoogleDriveServiceImpl(this, googleAuthManager)
    }

    val syncManager: com.cashflow.ai.data.sync.SyncManager by lazy {
        com.cashflow.ai.data.sync.SyncManager(
            transactionRepository = transactionRepository,
            sheetsService = googleSheetsService,
            driveService = googleDriveService,
            authManager = googleAuthManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Pre-populate default categories
        CoroutineScope(Dispatchers.IO).launch {
            transactionRepository.seedDefaultCategoriesIfEmpty()
        }

        // Schedule periodic background sync
        com.cashflow.ai.data.sync.work.SyncWorker.enqueuePeriodicSync(this)
        InsightWorker.enqueueWeekly(this)
    }

    companion object {
        lateinit var instance: CashFlowApp
            private set
    }
}
