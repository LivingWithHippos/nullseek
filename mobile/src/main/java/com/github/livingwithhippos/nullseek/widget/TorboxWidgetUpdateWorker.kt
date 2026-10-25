package com.github.livingwithhippos.nullseek.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import logcat.asLog
import logcat.logcat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class TorboxWidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params),
    KoinComponent {
    private val widgetRepository: TorboxWidgetRepository by inject()

    override suspend fun doWork(): Result =
        try {
            widgetRepository.refreshWidgetData()
            Result.success()
        } catch (e: Exception) {
            logcat("TorboxWidgetUpdateWorker") { "Worker failed: ${e.asLog()}" }
            Result.retry()
        }

    companion object {
        private const val PERIODIC_WORK_NAME = "torbox_widget_periodic_update"
        private const val IMMEDIATE_WORK_NAME = "torbox_widget_immediate_update"

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<TorboxWidgetUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun schedulePeriodic(context: Context) {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val request =
                PeriodicWorkRequestBuilder<TorboxWidgetUpdateWorker>(
                    15,
                    TimeUnit.MINUTES,
                ).setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
