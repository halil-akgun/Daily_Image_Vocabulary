package com.example.dailyimagevocabulary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersistentNotificationService : Service() {
    
    companion object {
        private const val CHANNEL_ID = AppConstants.CHANNEL_ID_PERSISTENT
        private const val NOTIFICATION_ID = AppConstants.NOTIFICATION_ID
        
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
                val repository = AppRepository(this@PersistentNotificationService)
                val image = repository.getCurrentImage()
                
                if (image != null) {
                    android.util.Log.d("PersistentNotificationService", "Creating initial notification with image: ${image.fileName}")
                    
                    val notification = NotificationBuilder.createImageNotification(
                        context = this@PersistentNotificationService,
                        image = image,
                        channelId = CHANNEL_ID,
                        isOngoing = true,
                        onlyAlertOnce = true
                    )
                    
                    // Switch to main thread for foreground service operations
                    withContext(Dispatchers.Main) {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    // No images - show basic notification
                    withContext(Dispatchers.Main) {
                        val notification = NotificationBuilder.createSimpleNotification(
                            context = this@PersistentNotificationService,
                            text = "No images in collection",
                            channelId = CHANNEL_ID,
                            isOngoing = true
                        )
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PersistentNotificationService", "Error creating initial notification", e)
                
                // Fallback notification
                withContext(Dispatchers.Main) {
                    val notification = NotificationBuilder.createSimpleNotification(
                        context = this@PersistentNotificationService,
                        text = "Loading...",
                        channelId = CHANNEL_ID,
                        isOngoing = true
                    )
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
                val repository = AppRepository(this@PersistentNotificationService)
                val image = repository.getCurrentImage()
                
                if (image != null) {
                    android.util.Log.d("PersistentNotificationService", "Updating notification with image: ${image.fileName}")
                    
                    val notification = NotificationBuilder.createImageNotification(
                        context = this@PersistentNotificationService,
                        image = image,
                        channelId = CHANNEL_ID,
                        isOngoing = true,
                        onlyAlertOnce = true
                    )
                    
                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                } else {
                    android.util.Log.w("PersistentNotificationService", "No images found")
                    
                    // No images - show appropriate message
                    val notification = NotificationBuilder.createSimpleNotification(
                        context = this@PersistentNotificationService,
                        text = "No images in collection",
                        channelId = CHANNEL_ID,
                        isOngoing = true,
                        onlyAlertOnce = true
                    )
                    
                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                android.util.Log.e("PersistentNotificationService", "Error updating notification", e)
                
                // Show error notification
                val notification = NotificationBuilder.createSimpleNotification(
                    context = this@PersistentNotificationService,
                    text = "Error loading image",
                    channelId = CHANNEL_ID,
                    isOngoing = true,
                    onlyAlertOnce = true
                )
                
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }
}
