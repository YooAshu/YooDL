package com.example.yoodl.ui.pages.homepage

import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yoodl.data.repository.DownloadRepository
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomePageVM @Inject constructor(
    private val repo: DownloadRepository
) : ViewModel() {
    private val baseDownloadDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "YooDL/youtube"
    )

    var loadingInfo by mutableStateOf(false)
    var streamInfo by mutableStateOf<VideoInfo?>(null)
    var availableFormats by mutableStateOf<List<VideoFormat>>(emptyList())
    var showDownloadSheet by mutableStateOf(false)

    fun getYTVideoInfo(url: String, onError: (Exception) -> Unit = {}, onSuccess: () -> Unit = {}) {
        loadingInfo = true
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().getInfo(url)
                }
                streamInfo = info
                loadVideoFormats()
                onSuccess()

            } catch (e: Exception) {
                onError(e)
            } finally {
                loadingInfo = false
            }
        }
    }

    private fun loadVideoFormats() {
        availableFormats = streamInfo?.formats
            ?.filter { format ->
                format.vcodec != null &&
                        format.vcodec != "none" &&
                        format.height != null &&
                        format.height > 0
            }
            ?.sortedByDescending { it.height }
            ?.distinctBy { it.height }
            ?: emptyList()
    }

    fun downloadFormat(url: String, format: VideoFormat?, isAudio: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val baseDownloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "YooDL/youtube"
                )

                val downloadDir = if (isAudio) {
                    File(baseDownloadDir, "audio")
                } else {
                    File(baseDownloadDir, "video")
                }
                downloadDir.mkdirs()

                val request = YoutubeDLRequest(url)

                request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                request.addOption("--retries", "10")
                request.addOption("--fragment-retries", "10")
                request.addOption("--no-mtime", "")
                request.addOption("--write-info-json", "")
                request.addOption("--add-metadata", "")

                if (isAudio) {
                    request.addOption("-f", "worst[acodec!=none]")
                    request.addOption("-x", "")
                    request.addOption("--audio-format", "mp3")
                    request.addOption("--audio-quality", "192")
                    request.addOption("--embed-thumbnail", "")
                    request.addOption("--write-thumbnail", "")  // Add this line
                    request.addOption("-o", "${downloadDir.absolutePath}/%(title)s [audio].%(ext)s")
                } else {
                    if (format != null) {
                        if (format.acodec == null || format.acodec == "none") {
                            request.addOption("-f", "${format.formatId}+bestaudio/best")
                        } else {
                            request.addOption("-f", format.formatId!!)
                        }
                        val quality = format.height?.let { "${it}p" } ?: "unknown"
                        request.addOption("--write-thumbnail", "")  // Add this line
                        request.addOption("-o", "${downloadDir.absolutePath}/%(title)s [${quality}].%(ext)s")
                    }
                }

                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                        Log.d("Download", "$progress% - ETA: ${eta}s - $line")
                    }
                }

                cleanupJsonFile(downloadDir)
                moveDownloadedThumbnails(downloadDir)
                onSuccess()

            } catch (e: Exception) {
                Log.e("Download", "Error: ${e.message}")
            }
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
            Log.d("Download", "Cleanup complete - deleted ${jsonFiles.size} json files")
        } catch (e: Exception) {
            Log.e("Download", "Error during cleanup: ${e.message}")
        }
    }

    private fun moveDownloadedThumbnails(downloadDir: File) {
        val thumbnailDir = File(baseDownloadDir, "thumbnails")
        thumbnailDir.mkdirs()

        downloadDir.listFiles()?.forEach { file ->
            if (isImageFile(file.name)) {
                file.renameTo(File(thumbnailDir, file.name))
            }
        }
    }
    private fun isImageFile(fileName: String): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
        return imageExtensions.any { fileName.endsWith(it, ignoreCase = true) }
    }

}
