package com.example.dailyimagevocabulary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat as CoreNotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "image_channel"

    fun showNotification(context: Context, image: ImageEntity) {

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Images",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }

            val bitmap = BitmapFactory.decodeFile(image.filePath)

            // Content intent to open ImageDetailActivity
            val contentIntent = Intent(context, ImageDetailActivity::class.java).apply {
                putExtra("image_id", image.id)
                putExtra("image_path", image.filePath)
                putExtra("image_name", image.fileName)
            }
            android.util.Log.d("NotificationHelper", "Creating intent for ImageDetailActivity with path: ${image.filePath}")
            val contentPendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                contentIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = CoreNotificationCompat.Builder(context, CHANNEL_ID)
                .setContentText(image.fileName.substringBeforeLast("."))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(CoreNotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentPendingIntent)
                .setStyle(CoreNotificationCompat.BigPictureStyle().bigPicture(bitmap))
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

            NotificationManagerCompat.from(context).notify(1, notification)
        }
    }
    
    // Get the current image that should be displayed
    suspend fun getCurrentImage(context: Context): ImageEntity? {
        return try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.dao()
            
            val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
            val collectionId = prefs.getInt("selectedCollectionId", 0)
            
            val images = dao.getImagesByCollection(collectionId)
            if (images.isNotEmpty()) {
                val index = prefs.getInt("index", 0)
                images.getOrNull(index) ?: images.first()
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error getting current image", e)
            null
        }
    }
    
    // Get the current image name for quick display
    suspend fun getCurrentImageName(context: Context): String {
        val image = getCurrentImage(context)
        return image?.fileName?.substringBeforeLast(".") ?: "No images in collection"
    }
}
