package com.cashflow.ai

import android.app.Application
import com.cashflow.ai.data.local.CashFlowDatabase
import com.cashflow.ai.data.repository.TransactionRepositoryImpl
import com.cashflow.ai.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Pre-populate default categories
        CoroutineScope(Dispatchers.IO).launch {
            transactionRepository.seedDefaultCategoriesIfEmpty()
        }
    }

    companion object {
        lateinit var instance: CashFlowApp
            private set
    }
}
