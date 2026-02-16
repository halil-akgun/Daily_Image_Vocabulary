package com.example.dailyimagevocabulary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File

object NotificationHelper {

    private const val CHANNEL_ID = "image_channel"

    fun showNotification(context: Context, image: ImageEntity) {

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Images",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val bitmap = BitmapFactory.decodeFile(image.filePath)

        // NEXT
        val nextIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "NEXT"
        }
        val nextPending = PendingIntent.getBroadcast(context, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE)

        // PREV
        val prevIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "PREV"
        }
        val prevPending = PendingIntent.getBroadcast(context, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE)

        // DELETE
        val deleteIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "DELETE"
        }
        val deletePending = PendingIntent.getBroadcast(context, 3, deleteIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Daily Item")
            .setContentText(image.word)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .addAction(android.R.drawable.ic_delete, "Delete", deletePending)
            .setOngoing(true)
            .build()

        NotificationManagerCompat.from(context).notify(1, notification)
    }
}
