package com.example.yoodl.ui.pages.downloads

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yoodl.data.models.DownloadItem
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.data.models.DownloadStatus
import com.example.yoodl.data.repository.DownloadRepository
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.text.format

@HiltViewModel
class DownloadPageVM @Inject constructor(
    private val repo: DownloadRepository
) : ViewModel() {

    private val baseDownloadDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "YooDL/youtube"
    )

    var downloadedItems by mutableStateOf<List<DownloadItem>>(emptyList())
    private set

    var currentDownload by mutableStateOf<DownloadQueue?>(null)
        private set

    var queuedDownloads by mutableStateOf<List<DownloadQueue>>(emptyList())
        private set

    var downloadProgress by mutableStateOf(0F)
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
        if (currentDownload == null) {
            processDownloadQueue()
        }
    }
    fun updateCurrentDownload(item: DownloadQueue?) {
        currentDownload = item
    }
    fun updateProgress(progress: Float, eta: Long) {
        downloadProgress = progress
        downloadETA = eta
    }
    fun removeFromQueue(downloadId: String) {
        queuedDownloads = queuedDownloads.filter { it.id != downloadId }
    }
    fun addToDownloadedItems(item: DownloadQueue, file: File?) {
        val thumbnailBitmap = if (item.isAudio) {
            // Audio files: embedded cover art
            repo.getEmbeddedThumbnail(file)
        } else {
            // Video files: extract first frame dynamically
            repo.getVideoFrame(file)
        }

        // ✅ Cache the thumbnail
        repo.cacheThumbnail(file, thumbnailBitmap)

        val newDownloadedItem = DownloadItem(
            id = item.id,
            title = item.title,
            filePath = file?.absolutePath ?: item.filePath,
            fileSize = 0L,
            dateAdded = System.currentTimeMillis(),
            type = if (item.isAudio) "audio" else "video",
            thumbnail = thumbnailBitmap
        )
        downloadedItems = listOf(newDownloadedItem) + downloadedItems
    }

    fun processDownloadQueue() {
        viewModelScope.launch {
            while (queuedDownloads.isNotEmpty()) {
                val currentItem = queuedDownloads.first()
                updateCurrentDownload(currentItem.copy(status = DownloadStatus.DOWNLOADING))
                val downloadDir = if (currentItem.isAudio) {
                    File(baseDownloadDir, "audio")
                } else {
                    File(baseDownloadDir, "video")
                }

                try {
                    val request = YoutubeDLRequest(currentItem.url)

                    request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    request.addOption("--retries", "10")
                    request.addOption("--fragment-retries", "10")
                    request.addOption("--no-mtime", "")
                    request.addOption("--write-info-json", "")
                    request.addOption("--add-metadata", "")

                    if (currentItem.isAudio) {
                        request.addOption("-f", "worst[acodec!=none]")
                        request.addOption("-x", "")
                        request.addOption("--audio-format", "mp3")
                        request.addOption("--audio-quality", "192")
                        request.addOption("--embed-thumbnail", "")
                        request.addOption("--write-thumbnail", "")
                        request.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

                    } else {
                        if (currentItem.format != null) {
                            if (currentItem.format.acodec == null || currentItem.format.acodec == "none") {
                                request.addOption("-f", "${currentItem.format.formatId}+bestaudio/best")
                            } else {
                                request.addOption("-f", currentItem.format.formatId!!)
                            }
                            val quality = currentItem.format.height?.let { "${it}p" } ?: "unknown"
                            request.addOption("--write-thumbnail", "")
                            request.addOption("-o", "${downloadDir.absolutePath}/%(title)s ${quality}.%(ext)s")
                        }
                    }

                    withContext(Dispatchers.IO) {
                        YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                            Log.d("Download", "$progress% - ETA: ${eta}s - $line")
                            updateProgress(progress, eta)
                        }
                    }


                    cleanupJsonFile(downloadDir)
                    deleteDownloadedThumbnails(downloadDir)
                    val outputFile = findLatestFile(downloadDir, if (currentItem.isAudio) "mp3" else "mp4")
                    addToDownloadedItems(currentItem, outputFile)

                    removeFromQueue(currentItem.id)


                } catch (e: Exception) {
                    Log.e("Download", "Error: ${e.message}")
                    updateCurrentDownload(currentItem.copy(status = DownloadStatus.FAILED))
                    removeFromQueue(currentItem.id)
                }
                updateProgress(0f, 0L)  // Add this before updateCurrentDownload(null)
                updateCurrentDownload(null)
                kotlinx.coroutines.delay(500)

            }
        }
    }

    fun findLatestFile(downloadDir: File, ext: String): File? {
        val files = downloadDir.listFiles()?.filter { it.extension.equals(ext, ignoreCase = true) } ?: return null
        return files.maxByOrNull { it.lastModified() }
    }

    private fun cleanupJsonFile(downloadDir: File) {
        try {
            val jsonFiles = downloadDir.listFiles { file ->
                file.name.endsWith(".info.json")
            } ?: emptyArray()

            jsonFiles.forEach { file ->
                file.delete()
            }
            Log.d("Download", "Cleanup complete - deleted ${jsonFiles.size} json files")
        } catch (e: Exception) {
            Log.e("Download", "Error during cleanup: ${e.message}")
        }
    }

    private fun deleteDownloadedThumbnails(downloadDir: File) {
        downloadDir.listFiles()
            ?.filter { it.extension in listOf("jpg", "png", "webp") }
            ?.forEach { it.delete() }
    }

    private fun isImageFile(fileName: String): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
        return imageExtensions.any { fileName.endsWith(it, ignoreCase = true) }
    }

}