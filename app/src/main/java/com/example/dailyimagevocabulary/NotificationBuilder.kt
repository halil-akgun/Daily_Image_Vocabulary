package com.example.dailyimagevocabulary

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat

object NotificationBuilder {
    
    fun createImageNotification(
        context: Context,
        image: ImageEntity,
        channelId: String,
        isOngoing: Boolean = false,
        onlyAlertOnce: Boolean = false,
        smallIcon: Int = android.R.drawable.ic_menu_gallery
    ): Notification {
        val bitmap = BitmapFactory.decodeFile(image.filePath)
        val contentIntent = createImageIntent(context, image)
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentText(image.fileName.substringBeforeLast("."))
            .setSmallIcon(smallIcon)
            .setContentIntent(contentIntent)
            .setOngoing(isOngoing)
            .setOnlyAlertOnce(onlyAlertOnce)
        
        // Add image if bitmap is successfully loaded
        if (bitmap != null) {
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
        }
        
        return builder.build()
    }
    
    fun createSimpleNotification(
        context: Context,
        text: String,
        channelId: String,
        isOngoing: Boolean = false,
        onlyAlertOnce: Boolean = false,
        smallIcon: Int = android.R.drawable.ic_menu_gallery
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentText(text)
            .setSmallIcon(smallIcon)
            .setOngoing(isOngoing)
            .setOnlyAlertOnce(onlyAlertOnce)
            .build()
    }
    
    fun createImageIntent(context: Context, image: ImageEntity): PendingIntent {
        val contentIntent = Intent(context, ImageDetailActivity::class.java).apply {
            putExtra("image_id", image.id)
            putExtra("image_path", image.filePath)
            putExtra("image_name", image.fileName)
        }
        
        return PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
