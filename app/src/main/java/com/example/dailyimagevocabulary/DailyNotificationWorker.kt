package com.example.dailyimagevocabulary

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.dao()

        val prefs = applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)
        val collectionId = prefs.getInt("selectedCollectionId", 0)

        val images = dao.getImagesByCollection(collectionId)
        if (images.isEmpty()) return Result.success()

        var index = prefs.getInt("index", 0)
        if (index >= images.size) index = 0

        val image = images[index]

        NotificationHelper.showNotification(applicationContext, image)

        return Result.success()
    }
}

