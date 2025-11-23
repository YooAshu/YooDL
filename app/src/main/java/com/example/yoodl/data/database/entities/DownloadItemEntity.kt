package com.example.yoodl.data.database.entities

// DownloadItemEntity.kt - Room Entity with all download information

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.example.yoodl.data.models.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadItemEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "status")
    val status: DownloadStatus = DownloadStatus.PENDING,

    @ColumnInfo(name = "progress")
    val progress: Int = 0,

    @ColumnInfo(name = "eta")
    val eta: Long = 0,

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "partial_file_path")
    val partialFilePath: String? = null,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "platform")
    val platform: String = "youtube",

    @ColumnInfo(name = "is_audio")
    val isAudio: Boolean,

    @ColumnInfo(name = "thumbnail")
    val thumbnail: String? = null,

    @ColumnInfo(name = "format")
    val formatId: String? = null,
    @ColumnInfo(name = "ext")
    val formatExt: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0
)
