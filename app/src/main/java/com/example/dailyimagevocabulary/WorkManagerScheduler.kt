package com.example.dailyimagevocabulary

import android.content.Context
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {
    
    private const val DAILY_WORK_TAG = AppConstants.DAILY_WORK_TAG
    
    fun scheduleDailyNotifications(context: Context) {
        // Calculate time until midnight
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1) // Next midnight
        }
        
        val initialDelay = midnight.timeInMillis - now.timeInMillis
        
        val workRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
                24, TimeUnit.HOURS
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
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
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}
