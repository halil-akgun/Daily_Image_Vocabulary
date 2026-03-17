package com.example.dailyimagevocabulary

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = AppRepository(applicationContext)
        val image = repository.getImagesForDailyNotification() ?: return Result.success()

        NotificationHelper.showNotification(applicationContext, image)
        
        // Refresh persistent notification service to show new image
        PersistentNotificationService.stopService(applicationContext)
        PersistentNotificationService.startService(applicationContext)

        return Result.success()
    }
}

