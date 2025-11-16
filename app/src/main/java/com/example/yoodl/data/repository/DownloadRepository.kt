package com.example.yoodl.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.example.yoodl.data.models.DownloadItem
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.yoodl.data.models.DownloadQueue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val baseDownloadDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "YooDL/youtube"
    )

    private val thumbnailDir = File(baseDownloadDir, "thumbnails")

    /**
     * Get all downloaded files (audio and video) - runs on IO thread
     */
    suspend fun getDownloadedItems(): List<DownloadItem> {
        return withContext(Dispatchers.IO) {
            val items = mutableListOf<DownloadItem>()

            try {
                // Get audio downloads
                val audioDir = File(baseDownloadDir, "audio")
                if (audioDir.exists()) {
                    items.addAll(scanDirectory(audioDir, "audio"))
                }

                // Get video downloads
                val videoDir = File(baseDownloadDir, "video")
                if (videoDir.exists()) {
                    items.addAll(scanDirectory(videoDir, "video"))
                }

                Log.d("DownloadRepository", "Found ${items.size} downloaded items")

                // Sort by dateAdded (newest first)
                items.sortedByDescending { it.dateAdded }
            } catch (e: Exception) {
                Log.e("DownloadRepository", "Error scanning downloads: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Scan a directory and extract file metadata with thumbnail
     */
    private fun scanDirectory(dir: File, type: String): List<DownloadItem> {
        val items = mutableListOf<DownloadItem>()

        dir.listFiles()?.forEach { file ->
            // Skip .info.json and image files
            if (file.isFile && !file.name.endsWith(".info.json") && !file.name.startsWith(".") && !isImageFile(file)) {
                val thumbnail = if(file.extension =="mp3") {
                    getEmbeddedThumbnail(file)
                } else {
                    val cached = loadCachedThumbnail( file)
                    cached ?: getVideoFrame(file)
                }



                items.add(
                    DownloadItem(
                        id = UUID.randomUUID().toString(),
                        title = file.nameWithoutExtension,
                        filePath = file.absolutePath,
                        fileSize = file.length(),
                        dateAdded = file.lastModified(),
                        type = type,
                        thumbnail = thumbnail,
                    )
                )
            }
        }

        return items
    }


    /**
     * Find thumbnail file in thumbnails folder
     */


    fun getEmbeddedThumbnail(file: File?): Bitmap? {
        if(file==null) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val data = retriever.embeddedPicture
            data?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    fun getVideoFrame(file: File?): Bitmap? {
        if (file == null || !file.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            // 0 microseconds = very first frame
            retriever.getFrameAtTime(0)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    fun cacheThumbnail(file: File?, thumbnailBitmap: Bitmap?) {
        if (file == null || thumbnailBitmap == null) return
        try {
            val cacheFile = File(context.cacheDir, "${file.nameWithoutExtension}.jpg")
            if (!cacheFile.exists()) {
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, cacheFile.outputStream())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun loadCachedThumbnail(file: File): Bitmap? {
        val cacheFile = File(context.cacheDir, "${file.nameWithoutExtension}.jpg")
        return if (cacheFile.exists()) BitmapFactory.decodeFile(cacheFile.absolutePath) else null
    }


    /**
     * Check if file is an image
     */
    private fun isImageFile(file: File): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
        return imageExtensions.any { file.name.endsWith(it, ignoreCase = true) }
    }

    /**
     * Delete a downloaded file and its thumbnail
     */
    suspend fun deleteDownload(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                val baseName = file.nameWithoutExtension
                file.delete()

                // Also delete associated thumbnail from thumbnails folder
                val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
                imageExtensions.forEach { ext ->
                    val thumbnailFile = File(thumbnailDir, "$baseName.$ext")
                    if (thumbnailFile.exists()) {
                        thumbnailFile.delete()
                    }
                }
                true
            } catch (e: Exception) {
                Log.e("DownloadRepository", "Error deleting file: ${e.message}")
                false
            }
        }
    }


    fun getFormattedFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun getFormattedDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }


}
