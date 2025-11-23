package com.example.yoodl.di

// DatabaseModule.kt - Hilt dependency injection setup

import android.content.Context
import com.example.yoodl.data.database.YooDLDatabase
import com.example.yoodl.data.database.dao.DownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideYooDLDatabase(
        @ApplicationContext context: Context
    ): YooDLDatabase {
        return YooDLDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideDownloadDao(
        database: YooDLDatabase
    ): DownloadDao {
        return database.downloadDao()
    }
}
