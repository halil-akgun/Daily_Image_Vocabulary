package com.example.dailyimagevocabulary

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {
    
    private const val DAILY_WORK_TAG = "daily_notification_work"
    
    fun scheduleDailyNotifications(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .addTag(DAILY_WORK_TAG)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    fun cancelDailyNotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(DAILY_WORK_TAG)
    }
}
