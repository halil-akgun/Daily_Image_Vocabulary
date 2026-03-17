package com.example.dailyimagevocabulary

object AppConstants {
    
    // Notification Channels
    const val CHANNEL_ID_PERSISTENT = "persistent_channel"
    const val CHANNEL_ID_IMAGE = "image_channel"
    
    // Notification IDs
    const val NOTIFICATION_ID = 1
    
    // SharedPreferences
    const val PREFS_NAME = "app"
    const val PREF_AUTO_START = "auto_start_prefs"
    const val PREF_SELECTED_COLLECTION_ID = "selectedCollectionId"
    const val PREF_IMAGE_INDEX = "index"
    const val PREF_PERMANENTLY_DISMISSED = "permanently_dismissed"
    
    // Work Manager
    const val DAILY_WORK_TAG = "daily_notification_work"
    
    // Request Codes
    const val REQUEST_CODE_BATTERY_OPTIMIZATION = 1001
}
