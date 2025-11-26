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
    val thumbnail: Bitmap?,
    val platform:String,
    val url: String,
    val status: DownloadStatus
)


//data class DownloadProgress(
//    val id: String,
//    val title: String,
//    val progress: Int,
//    val eta: Long,
//    val status: String
//)


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
    var thumbnail: String?,
    var platform: String = "youtube",
    var formatId: String? ,
    var formatExt: String?
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
    PAUSED
}

data class YtData(
    val id: String,
    val title: String,
    val url: String,
    val thumbnail: String,
    var duration: String = "",
    val channelName: String = "",
    val platform: String = "youtube"
)

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    object HideToast : UiEvent()
}

