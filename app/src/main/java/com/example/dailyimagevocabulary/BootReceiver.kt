package com.example.dailyimagevocabulary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Received action: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_PACKAGE_REPLACED || 
            intent.action == Intent.ACTION_PACKAGE_RESTARTED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Log.d("BootReceiver", "Starting notification services...")
            
            try {
                // Start notification services after boot
                WorkManagerScheduler.scheduleDailyNotifications(context)
                PersistentNotificationService.startService(context)
                
                Log.d("BootReceiver", "Notification services started successfully")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting notification services", e)
            }
        }
    }
}
