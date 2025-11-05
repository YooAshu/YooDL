package com.example.yoodl.data.repository

import com.example.yoodl.data.models.DownloadItem
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class DownloadRepository @Inject constructor() {

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
            if (!file.name.endsWith(".info.json") && !isImageFile(file) && file.isFile) {
                val thumbnail = findThumbnail(file.nameWithoutExtension) ?: extractThumbnailFromMediaFile(file)


                items.add(
                    DownloadItem(
                        id = UUID.randomUUID().toString(),
                        title = file.nameWithoutExtension,
                        filePath = file.absolutePath,
                        fileSize = file.length(),
                        dateAdded = file.lastModified(),
                        type = type,
                        thumbnail = thumbnail
                    )
                )
            }
        }

        return items
    }

    /**
     * Find thumbnail file in thumbnails folder
     */
    private fun findThumbnail(fileName: String): String? {
        val imageExtensions = listOf("jpg", "jpeg", "png", "webp")

        for (ext in imageExtensions) {
            val thumbnailFile = File(thumbnailDir, "$fileName.$ext")
            if (thumbnailFile.exists()) {
                return thumbnailFile.absolutePath
            }
        }

        return null
    }

    private fun extractThumbnailFromMediaFile(file: File): String? {
        return try {
            Log.d("DownloadRepository", "Extracting thumbnail from media file: ${file.absolutePath}")
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val embeddedPicture = retriever.embeddedPicture
            retriever.release()

            if (embeddedPicture != null) {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)

                if (bitmap != null) {
                    thumbnailDir.mkdirs()
                    val thumbnailFile = File(thumbnailDir, "${file.nameWithoutExtension}.png")
                    thumbnailFile.outputStream().use { output ->
                        android.graphics.Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                            .compress(android.graphics.Bitmap.CompressFormat.PNG, 80, output)
                    }
                    Log.d("DownloadRepository", "Thumbnail extracted: ${thumbnailFile.absolutePath}")
                    thumbnailFile.absolutePath
                } else {
                    Log.d("DownloadRepository", "Thumbnail not extracted: Bitmap is null")
                    null
                }
            } else {
                Log.d("DownloadRepository", "Thumbnail not extracted: Embedded picture is null")
                null
            }
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error extracting thumbnail: ${e.message}")
            null
        }
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
