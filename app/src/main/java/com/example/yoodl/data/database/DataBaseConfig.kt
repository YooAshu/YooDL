package com.example.yoodl.data.database

// YooDLDatabase.kt - Room Database configuration

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.yoodl.data.database.converters.DownloadStatusConverter
import com.example.yoodl.data.database.dao.DownloadDao
import com.example.yoodl.data.database.entities.DownloadItemEntity

@Database(
    entities = [DownloadItemEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(DownloadStatusConverter::class)
abstract class YooDLDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var instance: YooDLDatabase? = null

        fun getInstance(context: Context): YooDLDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    YooDLDatabase::class.java,
                    "yoodl_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                instance = newInstance
                newInstance
            }
        }
    }
}
