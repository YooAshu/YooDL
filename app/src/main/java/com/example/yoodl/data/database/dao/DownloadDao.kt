package com.example.yoodl.data.database.dao

// DownloadDao.kt - Data Access Object for database operations

import androidx.room.*
import com.example.yoodl.data.database.entities.DownloadItemEntity
import com.example.yoodl.data.models.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItemEntity)

    @Update
    suspend fun updateDownload(item: DownloadItemEntity)

    @Delete
    suspend fun deleteDownload(item: DownloadItemEntity)

    @Query("DELETE FROM downloads WHERE id = :downloadId")
    suspend fun deleteDownloadById(downloadId: String)

    @Query("SELECT * FROM downloads WHERE id = :downloadId")
    suspend fun getDownloadById(downloadId: String): DownloadItemEntity?

    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    fun getAllDownloads(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY created_at DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ")
    suspend fun getDownloadsByStatusSync(status: DownloadStatus): List<DownloadItemEntity>

    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY created_at DESC")
    fun getDownloadsByStatuses(statuses: List<DownloadStatus>): Flow<List<DownloadItemEntity>>

    @Query("""
        UPDATE downloads 
        SET status = :newStatus, updated_at = :updatedAt 
        WHERE id = :downloadId
    """)
    suspend fun updateDownloadStatus(
        downloadId: String,
        newStatus: DownloadStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE downloads 
        SET progress = :progress, eta = :eta, updated_at = :updatedAt 
        WHERE id = :downloadId
    """)
    suspend fun updateDownloadProgress(
        downloadId: String,
        progress: Float,
        eta: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE downloads 
        SET status = :status, error_message = :errorMessage, updated_at = :updatedAt
        WHERE id = :downloadId
    """)
    suspend fun updateDownloadError(
        downloadId: String,
        status: DownloadStatus = DownloadStatus.FAILED,
        errorMessage: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE downloads 
        SET status = :status, completed_at = :completedAt, updated_at = :updatedAt, file_path = :filePath
        WHERE id = :downloadId
    """)
    suspend fun markDownloadCompleted(
        downloadId: String,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        completedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
        filePath:String
    )

    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteDownloadsByStatus(status: DownloadStatus)

    @Query("SELECT COUNT(*) FROM downloads WHERE status = :status")
    suspend fun getCountByStatus(status: DownloadStatus): Int

    @Query("SELECT COUNT(*) FROM downloads")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM downloads WHERE status = :status LIMIT :limit OFFSET :offset")
    suspend fun getDownloadsPaginated(
        status: DownloadStatus,
        limit: Int,
        offset: Int
    ): List<DownloadItemEntity>

    @Query("SELECT * FROM downloads WHERE platform = :platform ORDER BY created_at DESC")
    fun getDownloadsByPlatform(platform: String): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads WHERE type = :type ORDER BY created_at DESC")
    fun getDownloadsByType(type: String): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads WHERE platform = :platform AND status = :status ORDER BY created_at DESC")
    fun getDownloadsByPlatformAndStatus(platform: String, status: DownloadStatus): Flow<List<DownloadItemEntity>>


    @Query("SELECT * FROM downloads WHERE platform = :platform AND type = :type ORDER BY created_at DESC")
    fun getDownloadsByPlatformAndType(platform: String, type: String): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM downloads WHERE platform = :platform AND status = :status AND type = :type ORDER BY created_at DESC")
    fun getDownloadsByPlatformStatusAndType(platform: String, status: DownloadStatus, type: String): Flow<List<DownloadItemEntity>>

    @Query("SELECT DISTINCT platform FROM downloads")
    suspend fun getAllPlatforms(): List<String>

    @Query("SELECT * FROM downloads WHERE status = :status AND created_at < :beforeTimestamp ORDER BY created_at DESC LIMIT :limit")
    suspend fun getDownloadsByStatusPaginated(
        status: DownloadStatus,
        beforeTimestamp: Long,
        limit: Int
    ): List<DownloadItemEntity>

    @Query("UPDATE downloads SET created_at = :createdAt WHERE id = :downloadId")
    suspend fun updateCreatedAtTimestamp(downloadId: String,createdAt: Long = System.currentTimeMillis())
}
