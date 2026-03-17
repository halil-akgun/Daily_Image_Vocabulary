package com.example.dailyimagevocabulary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val CHANNEL_ID = AppConstants.CHANNEL_ID_IMAGE

    fun showNotification(context: Context, image: ImageEntity) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            createNotificationChannelIfNeeded(context)
            
            val notification = NotificationBuilder.createImageNotification(
                context = context,
                image = image,
                channelId = CHANNEL_ID,
                isOngoing = true,
                smallIcon = R.drawable.ic_launcher_foreground
            )
            
            NotificationManagerCompat.from(context).notify(AppConstants.NOTIFICATION_ID, notification)
        }
    }
    
    private fun createNotificationChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Images",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }
    
    // Get the current image that should be displayed - delegated to AppRepository
    suspend fun getCurrentImage(context: Context): ImageEntity? {
        return AppRepository(context).getCurrentImage()
    }

}
