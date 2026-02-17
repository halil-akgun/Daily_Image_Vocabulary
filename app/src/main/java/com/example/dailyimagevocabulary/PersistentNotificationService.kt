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
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        
        // Start foreground immediately with a basic notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Daily Image")
            .setContentText("Loading...")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun updateNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(this@PersistentNotificationService)
                val dao = db.dao()
                
                val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
                val collectionId = prefs.getInt("selectedCollectionId", 0)
                
                val images = dao.getImagesByCollection(collectionId)
                if (images.isNotEmpty()) {
                    val index = prefs.getInt("index", 0)
                    val image = images.getOrNull(index) ?: images.first()
                    
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
                    
                    val notification = NotificationCompat.Builder(this@PersistentNotificationService, CHANNEL_ID)
                        .setContentText(image.fileName.substringBeforeLast("."))
                        .setSmallIcon(android.R.drawable.ic_menu_gallery)
                        .setContentIntent(contentPendingIntent)
                        .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .build()
                        
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                android.util.Log.e("PersistentNotificationService", "Error updating notification", e)
            }
        }
    }
}
