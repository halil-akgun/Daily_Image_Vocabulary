package com.example.dailyimagevocabulary

import android.content.Context
import android.util.Log

class AppRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.dao()
    private val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    
    suspend fun getCurrentImage(): ImageEntity? {
        return try {
            val collectionId = prefs.getInt(AppConstants.PREF_SELECTED_COLLECTION_ID, 0)
            val images = dao.getImagesByCollection(collectionId)
            
            if (images.isNotEmpty()) {
                val index = prefs.getInt(AppConstants.PREF_IMAGE_INDEX, 0)
                images.getOrNull(index) ?: images.first()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error getting current image", e)
            null
        }
    }
    
    suspend fun getCurrentImageName(): String {
        val image = getCurrentImage()
        return image?.fileName?.substringBeforeLast(".") ?: "No images in collection"
    }
    
    suspend fun getCollectionsWithCount(): List<Pair<CollectionEntity, Int>> {
        return try {
            val collections = dao.getCollections()
            val listWithCount = mutableListOf<Pair<CollectionEntity, Int>>()
            
            for (collection in collections) {
                val count = dao.getImageCount(collection.id)
                listWithCount.add(Pair(collection, count))
            }
            
            listWithCount
        } catch (e: Exception) {
            Log.e("AppRepository", "Error getting collections with count", e)
            emptyList()
        }
    }
    
    suspend fun getImagesForDailyNotification(): ImageEntity? {
        return try {
            val collectionId = prefs.getInt(AppConstants.PREF_SELECTED_COLLECTION_ID, 0)
            val images = dao.getImagesByCollection(collectionId)
            
            if (images.isEmpty()) return null
            
            var index = prefs.getInt(AppConstants.PREF_IMAGE_INDEX, 0)
            if (index >= images.size) index = 0
            
            val image = images[index]
            
            // Increment index for next day
            val nextIndex = (index + 1) % images.size
            prefs.edit().putInt(AppConstants.PREF_IMAGE_INDEX, nextIndex).apply()
            
            image
        } catch (e: Exception) {
            Log.e("AppRepository", "Error getting image for daily notification", e)
            null
        }
    }
    
    suspend fun addCollection(name: String) {
        try {
            dao.insertCollection(CollectionEntity(name = name))
        } catch (e: Exception) {
            Log.e("AppRepository", "Error adding collection", e)
        }
    }
    
    fun getSelectedCollectionId(): Int {
        return prefs.getInt(AppConstants.PREF_SELECTED_COLLECTION_ID, 0)
    }
    
    fun setSelectedCollectionId(collectionId: Int) {
        prefs.edit()
            .putInt(AppConstants.PREF_SELECTED_COLLECTION_ID, collectionId)
            .putInt(AppConstants.PREF_IMAGE_INDEX, 0) // Reset index when collection changes
            .apply()
    }
    
    fun isAutoStartPermanentlyDismissed(): Boolean {
        val autoStartPrefs = context.getSharedPreferences(AppConstants.PREF_AUTO_START, Context.MODE_PRIVATE)
        return autoStartPrefs.getBoolean(AppConstants.PREF_PERMANENTLY_DISMISSED, false)
    }
    
    fun setAutoStartPermanentlyDismissed() {
        val autoStartPrefs = context.getSharedPreferences(AppConstants.PREF_AUTO_START, Context.MODE_PRIVATE)
        autoStartPrefs.edit().putBoolean(AppConstants.PREF_PERMANENTLY_DISMISSED, true).apply()
    }
}
