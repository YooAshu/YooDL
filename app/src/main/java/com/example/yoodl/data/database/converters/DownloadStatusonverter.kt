package com.example.yoodl.data.database.converters


import androidx.room.TypeConverter
import com.example.yoodl.data.models.DownloadStatus

class DownloadStatusConverter {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toDownloadStatus(statusString: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(statusString)
        } catch (_: IllegalArgumentException) {
            DownloadStatus.FAILED
        }
    }
}