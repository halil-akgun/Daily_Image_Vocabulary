package com.example.dailyimagevocabulary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start notification services after boot
            WorkManagerScheduler.scheduleDailyNotifications(context)
            PersistentNotificationService.startService(context)
        } else if (intent.action == Intent.ACTION_PACKAGE_REPLACED || 
                   intent.action == Intent.ACTION_PACKAGE_RESTARTED ||
                   intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Restart services when app is updated or restarted
            WorkManagerScheduler.scheduleDailyNotifications(context)
            PersistentNotificationService.startService(context)
        }
    }
}
