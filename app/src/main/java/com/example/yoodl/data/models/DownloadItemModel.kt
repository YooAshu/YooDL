package com.example.yoodl.data.models

import android.graphics.Bitmap
import com.yausername.youtubedl_android.mapper.VideoFormat

data class DownloadItem(
    val id: String,
    val title: String,
    val filePath: String,
    val fileSize: Long,
    val dateAdded: Long,
    val type: String,
    val thumbnail: Bitmap?
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
    var thumbnail: String?
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class YtData(
    val id: String,
    val title: String,
    val url: String,
    val thumbnail: String,
    val duration: String = "",
    val channelName: String = "",
)

