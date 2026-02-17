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

            val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
            val collectionId = prefs.getInt("selectedCollectionId", 0)
            
            val images = dao.getImagesByCollection(collectionId)
            if (images.isEmpty()) return@launch

            var index = prefs.getInt("index", 0)

            when (intent.action) {
                "CHANGE" -> {
                    // Move to next image
                    index++
                    if (index >= images.size) index = 0
                }
                "DELETE" -> {
                    val image = images[index]
                    dao.deleteImage(image)
                    // Reset index to 0 after deletion
                    index = 0
                }
            }

            prefs.edit().putInt("index", index).apply()

            // Refresh images list after potential deletion
            val updatedImages = dao.getImagesByCollection(collectionId)
            if (updatedImages.isEmpty()) return@launch
            
            val finalIndex = if (index >= updatedImages.size) 0 else index
            val image = updatedImages[finalIndex]

            NotificationHelper.showNotification(context, image)
        }
    }
}
