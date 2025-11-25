package com.example.yoodl.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import com.example.yoodl.data.database.dao.DownloadDao
import com.example.yoodl.data.database.entities.DownloadItemEntity
import com.example.yoodl.data.models.DownloadItem
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.data.models.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class DownloadRepositoryV2 @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val baseDownloadDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "YooDL/youtube"
    )

    // ============ DOWNLOAD CRUD OPERATIONS ============

    suspend fun insertDownload(queue: DownloadQueue): String {
        withContext(Dispatchers.IO) {
            downloadDao.insertDownload(
                DownloadItemEntity(
                    id = queue.id,
                    title = queue.title,
                    url = queue.url,
                    filePath = queue.filePath,
                    fileSize = 0L,
                    type = if (queue.isAudio) "audio" else "video",
                    isAudio = queue.isAudio,
                    platform = queue.platform,
                    status = DownloadStatus.PENDING,
                    thumbnail = queue.thumbnail,
                    formatId = queue.formatId,
                    formatExt = queue.formatExt
                )
            )
        }
        return queue.id
    }


    suspend fun updateDownloadProgress(downloadId: String, progress: Float, eta: Long) {
        withContext(Dispatchers.IO) {
            downloadDao.updateDownloadProgress(downloadId, progress, eta)
        }
    }

    suspend fun markDownloadCompleted(downloadId: String, filepath: String) {
        withContext(Dispatchers.IO) {
            downloadDao.markDownloadCompleted(
                downloadId = downloadId,
                status = DownloadStatus.COMPLETED,
                completedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                filePath = filepath
            )
        }
    }

    suspend fun markDownloadFailed(downloadId: String, errorMessage: String?) {
        withContext(Dispatchers.IO) {
            downloadDao.updateDownloadError(
                downloadId = downloadId,
                status = DownloadStatus.FAILED,
                errorMessage = errorMessage
            )
        }
    }

    suspend fun markDownloadFailedToPending(downloadId: String) {
        withContext(Dispatchers.IO) {
            downloadDao.updateDownloadStatus(
                downloadId = downloadId,
                newStatus = DownloadStatus.PENDING
            )
        }
    }


    suspend fun updateDownloadStatus(downloadId: String, status: DownloadStatus) {
        withContext(Dispatchers.IO) {
            downloadDao.updateDownloadStatus(downloadId, status)
        }
    }

    suspend fun deleteDownload(downloadId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val entity = downloadDao.getDownloadById(downloadId)
                if (entity != null && entity.filePath.isNotEmpty()) {
                    val file = File(entity.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                downloadDao.deleteDownloadById(downloadId)
                true
            } catch (e: Exception) {
                Log.e("DownloadRepositoryV2", "Error deleting download: ${e.message}")
                false
            }
        }
    }


    // ============ QUERY OPERATIONS ============

    fun getAllDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.map { it.toDownloadItem(loadThumbnail(it.filePath, it.id)) }
        }
    }

    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>> {
        return downloadDao.getDownloadsByStatus(status).map { entities ->
            entities.map { it.toDownloadItem(loadThumbnail(it.filePath, it.id)) }
        }
    }

    suspend fun getDownloadsByStatusSync(status: DownloadStatus): List<DownloadItemEntity> {
        return withContext(Dispatchers.IO) {
            downloadDao.getDownloadsByStatusSync(status)
        }
    }


    fun getDownloadsByPlatform(platform: String): Flow<List<DownloadItem>> {
        return downloadDao.getDownloadsByPlatform(platform).map { entities ->
            entities.map { it.toDownloadItem(loadThumbnail(it.filePath, it.id)) }
        }
    }
    fun getDownloadsByType(type: String): Flow<List<DownloadItem>> {
        return downloadDao.getDownloadsByType(type).map { entities ->
            entities.map { it.toDownloadItem(loadThumbnail(it.filePath, it.id)) }
        }
    }


    fun getDownloadsByPlatformAndStatus(
        platform: String,
        status: DownloadStatus
    ): Flow<List<DownloadItem>> {
        return downloadDao.getDownloadsByPlatformAndStatus(platform, status).map { entities ->
            entities.map { it.toDownloadItem(loadThumbnail(it.filePath, it.id)) }
        }
    }

//    fun getDownloadsByType(type: String): Flow<List<DownloadItem>> {
//        return downloadDao.getAllDownloads().map { entities ->
//            entities.filter { it.type == type }
//                .map { it.toDownloadItem(loadThumbnail(it.filePath, it.id)) }
//        }
//    }

    suspend fun getQueueDownloadById(downloadId: String): DownloadQueue? {
        return withContext(Dispatchers.IO) {
            val entity = downloadDao.getDownloadById(downloadId)
            entity?.toQueueItem()
        }
    }
    suspend fun updateCreatedAtTimeStamp(downloadId: String) {
        withContext(Dispatchers.IO) {
            downloadDao.updateCreatedAtTimestamp(downloadId)
        }
    }

    // ============ THUMBNAIL MANAGEMENT ============

    fun getEmbeddedThumbnail(file: File?): Bitmap? {
        if (file == null) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val data = retriever.embeddedPicture
            data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            Log.e("Thumbnail", "Error extracting embedded thumbnail: ${e.message}")
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
            retriever.getFrameAtTime(0)
        } catch (e: Exception) {
            Log.e("Thumbnail", "Error extracting video frame: ${e.message}")
            null
        } finally {
            retriever.release()
        }
    }


    fun cacheThumbnail(file: File?, thumbnailBitmap: Bitmap?, videoId: String = "") {
        if (file == null || thumbnailBitmap == null) return
        try {
            val cacheKey = videoId.ifEmpty { file.nameWithoutExtension }
            val cacheFile = File(context.cacheDir, "$cacheKey.jpg")
            if (!cacheFile.exists()) {
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, cacheFile.outputStream())
                Log.d("Thumbnail", "Cached: ${cacheFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("Thumbnail", "Error caching: ${e.message}")
        }
    }

     fun deleteCachedThumbnail(videoId: String) {
        try {
            val cacheFile = File(context.cacheDir, "$videoId.jpg")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (e: Exception) {
            Log.e("Download", "Error deleting cached thumbnail: ${e.message}")
        }
    }

    private fun loadThumbnail(filePath: String, videoId: String = ""): Bitmap? {
        if (filePath.isEmpty()) return null
        val file = File(filePath)

        val cacheKey = videoId.ifEmpty { file.nameWithoutExtension }
        val cacheFile = File(context.cacheDir, "$cacheKey.jpg")

        return if (cacheFile.exists()) {
            Log.d("Thumbnail", "Loaded from cache: ${cacheFile.absolutePath}")
            BitmapFactory.decodeFile(cacheFile.absolutePath)
        } else {
            val thumbnail = if (file.extension == "mp3") {
                getEmbeddedThumbnail(file)
            } else {
                getVideoFrame(file)
            }
            if (thumbnail != null) {
                cacheThumbnail(file, thumbnail, videoId)
            }
            thumbnail
        }
    }


    // ============ UTILITY EXTENSIONS ============

    private fun DownloadItemEntity.toDownloadItem(thumbnail: Bitmap?): DownloadItem {
        return DownloadItem(
            id = id,
            title = title,
            filePath = filePath,
            fileSize = fileSize,
            dateAdded = createdAt,  // Change from created_at to createdAt
            type = type,
            thumbnail = thumbnail,
            platform = platform,
            status = status,
            url = url
        )
    }
    private fun DownloadItemEntity.toQueueItem(): DownloadQueue {
        return DownloadQueue(
            id = id,
            title = title,
            isAudio = isAudio,
            filePath = filePath,
            createdAt = System.currentTimeMillis() ,
            thumbnail = thumbnail,
            platform = platform,
            status = status,
            url = url,
            formatId = formatId,
            format = null,
            formatExt = formatExt
        )
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
