package com.example.yoodl.di

// DownloadRepositoryModule.kt - Add this to your existing DatabaseModule or create new

import android.content.Context
import com.example.yoodl.data.database.YooDLDatabase
import com.example.yoodl.data.database.dao.DownloadDao
import com.example.yoodl.data.repository.DownloadRepositoryV2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideDownloadRepositoryV2(
        @ApplicationContext context: Context,
        downloadDao: DownloadDao
    ): DownloadRepositoryV2 {
        return DownloadRepositoryV2(context, downloadDao)
    }
}
