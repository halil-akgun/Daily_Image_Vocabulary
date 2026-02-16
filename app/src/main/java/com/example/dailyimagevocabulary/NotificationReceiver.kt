package com.example.dailyimagevocabulary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val db = AppDatabase.getDatabase(context)
        val dao = db.dao()

        CoroutineScope(Dispatchers.IO).launch {

            val images = dao.getAllImages()
            if (images.isEmpty()) return@launch

            val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
            var index = prefs.getInt("index", 0)

            when (intent.action) {
                "NEXT" -> index++
                "PREV" -> index--
                "DELETE" -> {
                    val image = images[index]
                    dao.deleteImage(image)
                    index = 0
                }
            }

            if (index < 0) index = images.size - 1
            if (index >= images.size) index = 0

            prefs.edit().putInt("index", index).apply()

            val image = dao.getAllImages()[index]

            NotificationHelper.showNotification(context, image)
        }
    }
}
