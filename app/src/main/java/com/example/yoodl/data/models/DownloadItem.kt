package com.example.yoodl.data.models

import com.yausername.youtubedl_android.mapper.VideoFormat

data class DownloadItem(
    val id: String,
    val title: String,
    val filePath: String,
    val fileSize: Long,
    val dateAdded: Long,
    val type: String,
    val thumbnail: String?
)


data class DownloadProgress(
    val id: String,
    val title: String,
    val progress: Int,
    val eta: Long,
    val status: String
)


data class DownloadQueue(
    val id: String,
    val title: String,
    val url: String,
    val format: VideoFormat?,
    val isAudio: Boolean,
    val status: DownloadStatus,
    val progress: Int = 0,
    val eta: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val filePath: String,
    val thumbnail: String?
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
