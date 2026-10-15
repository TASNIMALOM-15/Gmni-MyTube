package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY addedTimestamp DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY addedTimestamp DESC")
    fun getFavoriteVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDownloaded = 1 ORDER BY addedTimestamp DESC")
    fun getDownloadedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE originalUrl = :url LIMIT 1")
    suspend fun getVideoByUrl(url: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' OR originalUrl LIKE '%' || :query || '%' ORDER BY addedTimestamp DESC")
    fun searchVideos(query: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET lastPositionMs = :position WHERE id = :id")
    suspend fun updatePlaybackPosition(id: String, position: Long)

    @Query("UPDATE videos SET thumbnailUrl = :thumb WHERE id = :id")
    suspend fun updateThumbnail(id: String, thumb: String)
    
    @Query("UPDATE videos SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("UPDATE videos SET isDownloaded = :isDown, localFilePath = :path WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDown: Boolean, path: String?)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)

    @Query("SELECT * FROM videos")
    suspend fun getAllVideosSnapshot(): List<VideoEntity>
}
