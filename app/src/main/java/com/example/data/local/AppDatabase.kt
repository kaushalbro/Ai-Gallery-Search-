package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AssetDao
import com.example.data.local.dao.FaceDao
import com.example.data.local.dao.JobDao
import com.example.data.local.dao.OcrDao
import com.example.data.local.dao.PersonDao
import com.example.data.local.dao.SearchDao
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.FaceEntity
import com.example.data.local.entity.IndexingJobEntity
import com.example.data.local.entity.OcrBlockEntity
import com.example.data.local.entity.OcrDocumentEntity
import com.example.data.local.entity.OcrSearchFtsEntity
import com.example.data.local.entity.PersonEntity

@Database(
    entities = [
        AssetEntity::class,
        OcrDocumentEntity::class,
        OcrBlockEntity::class,
        IndexingJobEntity::class,
        OcrSearchFtsEntity::class,
        FaceEntity::class,
        PersonEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun ocrDao(): OcrDao
    abstract fun jobDao(): JobDao
    abstract fun searchDao(): SearchDao
    abstract fun faceDao(): FaceDao
    abstract fun personDao(): PersonDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lensvault_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
