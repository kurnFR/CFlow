package com.cashflow.ai.data.sync.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cashflow.ai.CashFlowApp
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.MonthlyClose
import com.cashflow.ai.domain.usecase.GenerateFinancialInsightUseCase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class InsightWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? CashFlowApp ?: return Result.failure()
        return try {
            val repository = app.transactionRepository
            val range = if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1) {
                DateRange.LAST_MONTH
            } else {
                DateRange.THIS_MONTH
            }
            val summary = repository.getSummary(range).first()
            val categories = repository.getCategoryExpenses(range).first()
            val insight = GenerateFinancialInsightUseCase()(
                period = range.name,
                summary = summary,
                categoryExpenses = categories
            )
            val month = if (range == DateRange.LAST_MONTH) {
                DateUtils.getDateRangeBounds(DateRange.LAST_MONTH).first.substring(0, 7)
            } else {
                DateUtils.today().substring(0, 7)
            }
            repository.saveMonthlyClose(
                MonthlyClose(
                    month = month,
                    closedAt = if (range == DateRange.LAST_MONTH) System.currentTimeMillis() else null,
                    income = summary.totalIncome,
                    expense = summary.totalExpense,
                    net = summary.netBalance,
                    topExpenseCategory = categories.maxByOrNull { it.total }?.category,
                    insight = "${insight.headline}\n${insight.body}",
                    isAiGenerated = true,
                    generatedAt = System.currentTimeMillis()
                )
            )
            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "cashflow_weekly_insight"

        fun enqueueWeekly(context: Context) {
            val request = PeriodicWorkRequestBuilder<InsightWorker>(7, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
