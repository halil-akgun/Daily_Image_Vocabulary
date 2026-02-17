package com.example.dailyimagevocabulary

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CollectionEntity::class, ImageEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE images ADD COLUMN fileName TEXT NOT NULL DEFAULT ''
                """)
                database.execSQL("""
                    ALTER TABLE images ADD COLUMN dutchWord TEXT NOT NULL DEFAULT ''
                """)
                database.execSQL("""
                    ALTER TABLE images ADD COLUMN englishWord TEXT NOT NULL DEFAULT ''
                """)
            }
        }
        
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table without dutchWord and englishWord
                database.execSQL("""
                    CREATE TABLE images_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        collectionId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        fileName TEXT NOT NULL
                    )
                """)
                
                // Copy data from old table
                database.execSQL("""
                    INSERT INTO images_new (id, collectionId, filePath, fileName)
                    SELECT id, collectionId, filePath, fileName FROM images
                """)
                
                // Drop old table and rename new table
                database.execSQL("DROP TABLE images")
                database.execSQL("ALTER TABLE images_new RENAME TO images")
            }
        }
    }
}
