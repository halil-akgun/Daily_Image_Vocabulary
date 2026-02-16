package com.example.dailyimagevocabulary

import androidx.room.*

@Dao
interface AppDao {

    @Insert
    suspend fun insertCollection(collection: CollectionEntity)

    @Query("SELECT * FROM collections")
    suspend fun getCollections(): List<CollectionEntity>

    @Insert
    suspend fun insertImage(image: ImageEntity)

    @Query("SELECT * FROM images")
    suspend fun getAllImages(): List<ImageEntity>

    @Query("SELECT * FROM images WHERE collectionId = :collectionId")
    suspend fun getImagesByCollection(collectionId: Int): List<ImageEntity>

    @Delete
    suspend fun deleteImage(image: ImageEntity)

    @Query("SELECT COUNT(*) FROM images WHERE collectionId = :collectionId")
    suspend fun getImageCount(collectionId: Int): Int
}
