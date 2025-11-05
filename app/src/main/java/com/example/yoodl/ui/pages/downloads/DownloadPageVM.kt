package com.example.yoodl.ui.pages.downloads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yoodl.data.models.DownloadItem
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.data.repository.DownloadRepository
import com.yausername.youtubedl_android.mapper.VideoFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadPageVM @Inject constructor(
    private val repo: DownloadRepository
) : ViewModel() {

    var downloadedItems by mutableStateOf<List<DownloadItem>>(emptyList())
    private set

    var currentDownload by mutableStateOf<DownloadQueue?>(null)
        private set

    var queuedDownloads by mutableStateOf<List<DownloadQueue>>(emptyList())
        private set

    var downloadProgress by mutableStateOf(0)
        private set

    var downloadETA by mutableStateOf(0L)
        private set

    init {
        getDownloadedItems()
    }

    fun getDownloadedItems() {
        viewModelScope.launch {
            downloadedItems = repo.getDownloadedItems()
        }
    }
    fun getFormattedDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun addToQueue(item: DownloadQueue) {
        queuedDownloads = queuedDownloads + item
    }
    fun updateCurrentDownload(item: DownloadQueue?) {
        currentDownload = item
    }
    fun updateProgress(progress: Int, eta: Long) {
        downloadProgress = progress
        downloadETA = eta
    }
    fun removeFromQueue(downloadId: String) {
        queuedDownloads = queuedDownloads.filter { it.id != downloadId }
    }
    fun addToDownloadedItems(item: DownloadQueue) {
        val newDownloadedItem = DownloadItem(
            id = item.id,
            title = item.title,
            filePath = item.filePath, // You can set the actual file path here
            fileSize = 0L, // You can set the actual file size here
            dateAdded = System.currentTimeMillis(),
            type = if (item.isAudio) "audio" else "video",
            thumbnail = item.thumbnail // You can set the actual thumbnail path here
        )
        downloadedItems = downloadedItems + newDownloadedItem
    }
}