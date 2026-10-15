package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey
    val id: String,
    val videoId: String,
    val title: String,
    val downloadUrl: String,
    val targetFilePath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBps: Long = 0L,
    val etaSeconds: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    val speedFormatted: String
        get() = when {
            speedBps >= 1024 * 1024 -> String.format("%.2f MB/s", speedBps / (1024.0 * 1024.0))
            speedBps >= 1024 -> String.format("%.1f KB/s", speedBps / 1024.0)
            else -> "$speedBps B/s"
        }

    val sizeFormatted: String
        get() {
            val totalMb = totalBytes / (1024.0 * 1024.0)
            val downMb = downloadedBytes / (1024.0 * 1024.0)
            return if (totalBytes > 0) {
                String.format("%.1f MB / %.1f MB", downMb, totalMb)
            } else {
                String.format("%.1f MB", downMb)
            }
        }
}
