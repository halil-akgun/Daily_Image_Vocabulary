package com.example.dailyimagevocabulary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersistentNotificationService : Service() {
    
    companion object {
        private const val CHANNEL_ID = "persistent_channel"
        private const val NOTIFICATION_ID = 1
        
        fun startService(context: Context) {
            val intent = Intent(context, PersistentNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, PersistentNotificationService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        // Create notification with image immediately
        createNotificationWithCurrentImage()
    }
    
    private fun createNotificationWithCurrentImage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val image = NotificationHelper.getCurrentImage(this@PersistentNotificationService)
                
                if (image != null) {
                    android.util.Log.d("PersistentNotificationService", "Creating initial notification with image: ${image.fileName}")
                    
                    // Load the bitmap from file
                    val bitmap = BitmapFactory.decodeFile(image.filePath)
                    
                    // Content intent to open ImageDetailActivity
                    val contentIntent = Intent(this@PersistentNotificationService, ImageDetailActivity::class.java).apply {
                        putExtra("image_id", image.id)
                        putExtra("image_path", image.filePath)
                        putExtra("image_name", image.fileName)
                    }
                    val contentPendingIntent = PendingIntent.getActivity(
                        this@PersistentNotificationService, 
                        0, 
                        contentIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    val notificationBuilder = NotificationCompat.Builder(this@PersistentNotificationService, CHANNEL_ID)
                        .setContentText(image.fileName.substringBeforeLast("."))
                        .setSmallIcon(android.R.drawable.ic_menu_gallery)
                        .setContentIntent(contentPendingIntent)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                    
                    // Add image if bitmap is successfully loaded
                    if (bitmap != null) {
                        notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                        android.util.Log.d("PersistentNotificationService", "Initial notification created with image: ${image.fileName}")
                    } else {
                        android.util.Log.w("PersistentNotificationService", "Initial notification created without image (bitmap null)")
                    }
                    
                    val notification = notificationBuilder.build()
                    
                    // Switch to main thread for foreground service operations
                    withContext(Dispatchers.Main) {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    // No images - show basic notification
                    withContext(Dispatchers.Main) {
                        val notification = NotificationCompat.Builder(this@PersistentNotificationService, CHANNEL_ID)
                            .setContentText("No images in collection")
                            .setSmallIcon(android.R.drawable.ic_menu_gallery)
                            .setOngoing(true)
                            .build()
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PersistentNotificationService", "Error creating initial notification", e)
                
                // Fallback notification
                withContext(Dispatchers.Main) {
                    val notification = NotificationCompat.Builder(this@PersistentNotificationService, CHANNEL_ID)
                        .setContentText("Loading...")
                        .setSmallIcon(android.R.drawable.ic_menu_gallery)
                        .setOngoing(true)
                        .build()
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Update notification with actual content
        updateNotification()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Vocabulary",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun updateNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val image = NotificationHelper.getCurrentImage(this@PersistentNotificationService)
                
                if (image != null) {
                    android.util.Log.d("PersistentNotificationService", "Updating notification with image: ${image.fileName}")
                    
                    // Load the bitmap from file
                    val bitmap = BitmapFactory.decodeFile(image.filePath)
                    
                    if (bitmap == null) {
                        android.util.Log.w("PersistentNotificationService", "Failed to load bitmap from path: ${image.filePath}")
                    }
                    
                    // Content intent to open ImageDetailActivity
                    val contentIntent = Intent(this@PersistentNotificationService, ImageDetailActivity::class.java).apply {
                        putExtra("image_id", image.id)
                        putExtra("image_path", image.filePath)
                        putExtra("image_name", image.fileName)
                    }
                    val contentPendingIntent = PendingIntent.getActivity(
                        this@PersistentNotificationService, 
                        0, 
                        contentIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    val notificationBuilder = NotificationCompat.Builder(this@PersistentNotificationService, CHANNEL_ID)
                        .setContentText(image.fileName.substringBeforeLast("."))
                        .setSmallIcon(android.R.drawable.ic_menu_gallery)
                        .setContentIntent(contentPendingIntent)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                    
                    // Add image if bitmap is successfully loaded
                    if (bitmap != null) {
                        notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                        android.util.Log.d("PersistentNotificationService", "Notification updated with image: ${image.fileName}")
                    } else {
                        android.util.Log.w("PersistentNotificationService", "Notification updated without image (bitmap null)")
                    }
                    
                    val notification = notificationBuilder.build()
                        
                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                } else {
                    android.util.Log.w("PersistentNotificationService", "No images found")
                    
                    // No images - show appropriate message
                    val notification = NotificationCompat.Builder(this@PersistentNotificationService, CHANNEL_ID)
                        .setContentText("No images in collection")
                        .setSmallIcon(android.R.drawable.ic_menu_gallery)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .build()
                        
                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                android.util.Log.e("PersistentNotificationService", "Error updating notification", e)
                
                // Show error notification
                val notification = NotificationCompat.Builder(this@PersistentNotificationService, CHANNEL_ID)
                    .setContentText("Error loading image")
                    .setSmallIcon(android.R.drawable.ic_menu_gallery)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
                    
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }
}
