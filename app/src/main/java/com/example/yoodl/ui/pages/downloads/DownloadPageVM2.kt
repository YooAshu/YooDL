package com.example.yoodl.ui.pages.downloads

import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yoodl.data.models.DownloadItem
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.data.models.DownloadStatus
import com.example.yoodl.data.models.UiEvent
import com.example.yoodl.data.repository.DownloadRepositoryV2
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadPageVM @Inject constructor(
    private val dbRepo: DownloadRepositoryV2
) : ViewModel() {


    val downloadedItems: StateFlow<List<DownloadItem>> =
        dbRepo.getDownloadsByStatus(DownloadStatus.COMPLETED)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )


    var currentDownload by mutableStateOf<DownloadQueue?>(null)
        private set
    private var currentProcessId: String? = null
    var queuedDownloads by mutableStateOf<List<DownloadQueue>>(emptyList())
        private set

    var failedDownloads by mutableStateOf<List<DownloadQueue>>(emptyList())
        private set

    var downloadedItemByType by mutableStateOf<List<DownloadItem>>(emptyList())
        private set

    var showingSpecificType by mutableStateOf(false)
    var specificType by mutableStateOf("")
    var loadingSpecificType by mutableStateOf(false)
    var nameOfShowingType by mutableStateOf("")


    var downloadProgress by mutableStateOf(0F)
        private set

    var downloadETA by mutableStateOf(0L)
        private set

    init {
//        loadDownloadsFromDatabase()
        loadQueuedDownloads()
        loadFailedDownloads()

    }

    fun getFormattedDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun addToQueue(item: DownloadQueue) {
        viewModelScope.launch {
            dbRepo.insertDownload(item)
            queuedDownloads = queuedDownloads + item
//            Log.d("Download", "Inserted: ${item.id} with status ${item.status}")
            if (currentDownload == null) {
                processDownloadQueue()
            }
        }
    }

    fun updateCurrentDownload(item: DownloadQueue?) {
        currentDownload = item
    }

    fun updateProgress(progress: Float, eta: Long) {
        downloadProgress = progress
        downloadETA = eta
    }

    fun updateQueuedDownloadStatus(downloadId: String, newStatus: DownloadStatus) {
        queuedDownloads = queuedDownloads.map { item ->
            if (item.id == downloadId) {
                item.copy(status = newStatus)
            } else {
                item
            }
        }
        viewModelScope.launch {
            dbRepo.updateDownloadStatus(downloadId, newStatus)
        }
    }

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            // Destroy the YoutubeDL process first
            currentProcessId?.let {
                YoutubeDL.getInstance().destroyProcessById(it)
//                Log.d("Download", "Destroyed process: $it")
            }
            currentProcessId = null
            val pausedDownload = queuedDownloads.find { it.id == downloadId }
                ?.copy(status = DownloadStatus.PAUSED, createdAt = System.currentTimeMillis())
            if (pausedDownload != null)
                queuedDownloads = queuedDownloads + pausedDownload
            dbRepo.updateCreatedAtTimeStamp(downloadId = downloadId)
            dbRepo.updateDownloadStatus(downloadId = downloadId, status = DownloadStatus.PAUSED)
            updateCurrentDownload(null)
            updateProgress(0f, 0L)
        }
    }


    fun removeFromQueue(downloadId: String) {
        queuedDownloads = queuedDownloads.filter { it.id != downloadId }
    }

    fun processDownloadQueue() {
        viewModelScope.launch {
            while (queuedDownloads.any { it.status == DownloadStatus.PENDING }) {
                val currentItem =
                    queuedDownloads.firstOrNull { it.status == DownloadStatus.PENDING }
                        ?: run {
                            Log.d("Download", "No pending downloads")
                            return@launch
                        }
//                Log.d("Download", "processing...")
                queuedDownloads = queuedDownloads.map { item ->
                    if (item.id == currentItem.id) {
                        item.copy(status = DownloadStatus.DOWNLOADING)
                    } else {
                        item
                    }
                }
                //updated currentDownloaded
                updateCurrentDownload(currentItem.copy(status = DownloadStatus.DOWNLOADING))

                // ✅ Insert into database at start
                dbRepo.updateDownloadStatus(currentItem.id, DownloadStatus.DOWNLOADING)

                val baseDownloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "YooDL/${currentItem.platform}"
                )

                val downloadDir = if (currentItem.isAudio) {
                    File(baseDownloadDir, "audio")
                } else {
                    File(baseDownloadDir, "video")
                }
                downloadDir.mkdirs()
                try {
                    val request = YoutubeDLRequest(currentItem.url)

                    request.addOption(
                        "--user-agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    )
                    request.addOption("--retries", "10")
                    request.addOption("--fragment-retries", "10")
                    request.addOption("--no-mtime", "")
                    request.addOption("--write-info-json", "")
                    request.addOption("--add-metadata", "")

                    if (currentItem.isAudio) {
                        if (currentItem.formatId == null || currentItem.formatId == "") {
                            request.addOption("-f", "bestaudio/best")
                        } else {
                            request.addOption("-f", currentItem.formatId!!)
                        }
                        request.addOption("-x")
                        request.addOption("--audio-format", "mp3")
                        request.addOption("--audio-quality", "192")
                        request.addOption("--embed-thumbnail")
                        request.addOption("--write-thumbnail")
                        request.addOption("--convert-thumbnails", "jpg")
                        request.addOption(
                            "-o",
                            "${downloadDir.absolutePath}/%(title)s-${currentItem.id}.%(ext)s"
                        )
                    } else {
                        if (currentItem.formatId != null) {
                            request.addOption(
                                "-f",
                                "${currentItem.formatId}+bestaudio/best"
                            )
                        } else {
                            request.addOption(
                                "-f",
                                "bestvideo[height<=720]+bestaudio/best[height<=720]"
                            )
                        }

                        request.addOption(
                            "-o",
                            "${downloadDir.absolutePath}/%(title)s-${currentItem.id}.%(ext)s"
                        )

                    }
                    currentProcessId = currentItem.id

                    withContext(Dispatchers.IO) {
                        YoutubeDL.getInstance()
                            .execute(request, currentItem.id) { progress, eta, line ->
//                                Log.d("Download", "$progress% - ETA: ${eta}s - $line")
                                updateProgress(progress, eta)
                            }
                    }

                    Log.d("Download", "continuing after cancel")
                    withContext(Dispatchers.IO) {
                        cleanupJsonFile(downloadDir)
                        deleteDownloadedThumbnails(downloadDir)
                    }
                    val outputFile = withContext(Dispatchers.IO) {
                        findLatestFile(downloadDir, currentItem.formatExt!!)
                    }
                    showToast("Downloaded SuccessFully")
                    // ✅ Cache thumbnail and mark as completed
                    if (outputFile != null) {
                        withContext(Dispatchers.IO) {
                            val thumbnail = if (currentItem.isAudio) {
                                dbRepo.getEmbeddedThumbnail(outputFile)
                            } else {
                                dbRepo.getVideoFrame(outputFile)
                            }
                            dbRepo.cacheThumbnail(outputFile, thumbnail, currentItem.id)
                        }
                    }
                    //update filepath
                    updateCurrentDownload(
                        currentItem.copy(
                            filePath = outputFile?.absolutePath ?: currentItem.filePath
                        )
                    )
                    // ✅ Mark as completed in database
                    withContext(Dispatchers.IO) {
                        dbRepo.markDownloadCompleted(
                            currentItem.id,
                            outputFile?.absolutePath ?: currentItem.filePath
                        )
                    }


                    removeFromQueue(currentItem.id)

                } catch (_: YoutubeDL.CanceledException) {
                    Log.d("Download", "Download process cancelled")
                    removeFromQueue(currentItem.id)
                    cleanupJsonFile(downloadDir)
                    deleteDownloadedThumbnails(downloadDir)
                } catch (e: Exception) {
                    Log.e("Download", "Error: ${e.message}")
                    updateCurrentDownload(currentItem.copy(status = DownloadStatus.FAILED))

                    // ✅ Clean up on failure
                    cleanupJsonFile(downloadDir)
                    deleteDownloadedThumbnails(downloadDir)
                    dbRepo.deleteCachedThumbnail(currentItem.id)
                    removeFromQueue(currentItem.id)
                    failedDownloads = failedDownloads + currentItem
                    dbRepo.markDownloadFailed(currentItem.id, e.message)
                }
                updateProgress(0f, 0L)
                kotlinx.coroutines.delay(500)

            }

            updateCurrentDownload(null)
        }
//        updateProgress(0f, 0L)
//        updateCurrentDownload(null)
    }

    fun findLatestFile(downloadDir: File, ext: String): File? {
        val files = downloadDir.listFiles()?.filter { it.extension.equals(ext, ignoreCase = true) }
            ?: return null
        return files.maxByOrNull { it.lastModified() }
    }

//    fun retryFailedDownload(downloadId: String) {
//        val failedItem = failedDownloads.find { it.id == downloadId }
//        if (failedItem != null) {
//            // Remove from failed list and add back to queue
//            failedDownloads = failedDownloads.filter { it.id != downloadId }
//
//            // Reset status and retry count for new attempt
//            val retryItem = failedItem.copy(status = DownloadStatus.PENDING)
//            addToQueue(retryItem)
//        }
//    }

//    fun removeFailedDownload(downloadId: String) {
//        failedDownloads = failedDownloads.filter { it.id != downloadId }
//        viewModelScope.launch {
//            dbRepo.deleteDownload(downloadId)
//        }
//    }


    private fun loadQueuedDownloads() {
        viewModelScope.launch {
//            Log.d("Download", "Starting loadQueuedDownloads")

            // 1. Find any stalled downloads (stuck as DOWNLOADING)
            val stalledDownloads = dbRepo.getDownloadsByStatusSync(DownloadStatus.DOWNLOADING)
            if (stalledDownloads.isNotEmpty()) {

                stalledDownloads.forEach { stalled ->
                    dbRepo.updateDownloadStatus(stalled.id, DownloadStatus.PENDING)
//                    Log.d("Download", "Reset ${stalled.title} to PENDING")
                }

            }

            // 2. Load all PENDING downloads (including the ones we just reset)
            val pendingDownloads = dbRepo.getDownloadsByStatusSync(DownloadStatus.PENDING)
//            Log.d("Download", "Raw query result: ${pendingDownloads.size} items")

            pendingDownloads.forEach { entity ->
                dbRepo.updateDownloadStatus(entity.id, DownloadStatus.PAUSED)
            }

            val pausedDownloads = dbRepo.getDownloadsByStatusSync(DownloadStatus.PAUSED)

            queuedDownloads = pausedDownloads.map { entity ->
                DownloadQueue(
                    id = entity.id,
                    title = entity.title,
                    url = entity.url,
                    format = null,
                    isAudio = entity.isAudio,
                    status = DownloadStatus.PAUSED,
                    progress = entity.progress,
                    eta = entity.eta,
                    createdAt = entity.createdAt,
                    filePath = entity.filePath,
                    thumbnail = entity.thumbnail,
                    platform = entity.platform,
                    formatId = entity.formatId,
                    formatExt = entity.formatExt
                )
            }
//            Log.d("Download", "Queued downloads loaded: ${queuedDownloads.size}")
//            if (queuedDownloads.isNotEmpty()) {
////                processDownloadQueue()
//            }
        }
    }

    private fun loadFailedDownloads() {
        viewModelScope.launch {
            val failedDownloadsEntity = dbRepo.getDownloadsByStatusSync(DownloadStatus.FAILED)
            failedDownloads = failedDownloadsEntity.map { entity ->
                DownloadQueue(
                    id = entity.id,
                    title = entity.title,
                    url = entity.url,
                    format = null,
                    isAudio = entity.isAudio,
                    status = entity.status,
                    progress = entity.progress,
                    eta = entity.eta,
                    createdAt = entity.createdAt,
                    filePath = entity.filePath,
                    thumbnail = entity.thumbnail,
                    platform = entity.platform,
                    formatId = entity.formatId,
                    formatExt = entity.formatExt
                )
            }
        }
    }

    fun moveFromFailedToQueued(downloadId: String) {
        viewModelScope.launch {
            dbRepo.updateCreatedAtTimeStamp(downloadId = downloadId)
            dbRepo.updateDownloadStatus(downloadId = downloadId, status = DownloadStatus.PENDING)
            val failedDownload = failedDownloads.find { it.id == downloadId }
                ?.copy(status = DownloadStatus.PENDING, createdAt = System.currentTimeMillis())

            if (failedDownload != null) {
                queuedDownloads = queuedDownloads + failedDownload
                failedDownloads = failedDownloads.filter { it.id != downloadId }
            }
            if (currentDownload == null) processDownloadQueue()
        }
    }


    private fun cleanupJsonFile(downloadDir: File) {
        try {
            val jsonFiles = downloadDir.listFiles { file ->
                file.name.endsWith(".info.json")
            } ?: emptyArray()

            jsonFiles.forEach { file ->
                file.delete()
            }
//            Log.d("Download", "Cleanup complete - deleted ${jsonFiles.size} json files")
        } catch (e: Exception) {
            Log.e("Download", "Error during cleanup: ${e.message}")
        }
    }

    private fun deleteDownloadedThumbnails(downloadDir: File) {
        downloadDir.listFiles()
            ?.filter { it.extension in listOf("jpg", "png", "webp") }
            ?.forEach { it.delete() }
    }


//    fun logAllDownloads() {
//        viewModelScope.launch {
//            val allDownloads = dbRepo.getAllDownloads()
//            allDownloads.collect { it ->
//                it.forEach { item ->
//                    Log.d(
//                        "Download",
//                        "Title: ${item.title} - Status: ${item.status} -platform ${item.platform} type: ${item.type}"
//                    )
//                }
//            }
//        }
//    }

    fun getDownloadedItemByType(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loadingSpecificType = true
            nameOfShowingType = type
            when (type) {
                "audio", "video" -> {
                    showingSpecificType = true
                    downloadedItemByType = dbRepo.getDownloadsByType(type).first()
                }

                "instagram", "facebook", "youtube" -> {
                    showingSpecificType = true
                    downloadedItemByType = dbRepo.getDownloadsByPlatform(type).first()
                }

                "all" -> {
                    showingSpecificType = false
                    downloadedItemByType = emptyList()
                }
            }
            loadingSpecificType = false
        }
    }


    fun removeDownload(id: String,onDelete: () -> Unit){
        viewModelScope.launch {
            if(dbRepo.deleteDownload(id)) onDelete()
        }
    }
    fun removeTypeDownload(id: String,onDelete: () -> Unit){
        viewModelScope.launch {
            if(dbRepo.deleteDownload(id)) {
                onDelete()
                downloadedItemByType = downloadedItemByType.filter { it.id != id }
            }
        }
    }

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent: StateFlow<UiEvent?> = _uiEvent.asStateFlow()

    fun showToast(message: String) {
        _uiEvent.value = UiEvent.ShowToast(message)
    }

    fun clearToastEvent() {
        _uiEvent.value = null
    }

}
