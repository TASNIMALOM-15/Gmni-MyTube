package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt ASC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadItem?

    @Query("SELECT * FROM downloads WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownloadByVideoId(videoId: String): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem)

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Query("UPDATE downloads SET status = :status, errorReason = :reason WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus, reason: String? = null)

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, totalBytes = :total, speedBps = :speed, etaSeconds = :eta WHERE id = :id")
    suspend fun updateProgress(id: String, downloaded: Long, total: Long, speed: Long, eta: Long)

    @Delete
    suspend fun deleteDownload(download: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)
}
