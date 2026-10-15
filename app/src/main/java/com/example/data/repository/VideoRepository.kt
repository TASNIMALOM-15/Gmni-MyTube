package com.example.data.repository

import android.content.Context
import com.example.data.local.DownloadDao
import com.example.data.local.PlaylistDao
import com.example.data.local.VideoDao
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.PlaylistEntity
import com.example.data.model.VideoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class VideoRepository(
    private val context: Context,
    private val videoDao: VideoDao,
    private val playlistDao: PlaylistDao,
    private val downloadDao: DownloadDao,
    private val linkResolver: LinkResolver = LinkResolver()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val favoriteVideos: Flow<List<VideoEntity>> = videoDao.getFavoriteVideos()
    val downloadedVideos: Flow<List<VideoEntity>> = videoDao.getDownloadedVideos()
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    fun searchVideos(query: String): Flow<List<VideoEntity>> = videoDao.searchVideos(query)

    suspend fun getVideoById(id: String): VideoEntity? = videoDao.getVideoById(id)

    suspend fun addVideoFromUrl(
        url: String,
        customTitle: String? = null,
        targetPlaylist: String? = null
    ): Result<VideoEntity> = withContext(Dispatchers.IO) {
        val existing = videoDao.getVideoByUrl(url)
        if (existing != null) {
            return@withContext Result.success(existing)
        }

        val resolveResult = linkResolver.resolve(url)
        val info = resolveResult.getOrNull()

        val finalTitle = customTitle?.takeIf { it.isNotBlank() }
            ?: info?.title
            ?: "Video_${System.currentTimeMillis() % 10000}"

        val streamUrl = info?.streamUrl ?: url
        val thumb = info?.thumbnailUrl ?: ""
        val domain = info?.domain ?: ""

        val playlistString = if (!targetPlaylist.isNullOrBlank()) targetPlaylist else ""

        val video = VideoEntity(
            id = UUID.randomUUID().toString(),
            title = finalTitle,
            originalUrl = url,
            streamUrl = streamUrl,
            thumbnailUrl = thumb,
            sourceDomain = domain,
            playlistNames = playlistString
        )

        videoDao.insertVideo(video)
        // ভিডিও লিস্টে সাথে সাথে দেখা যাবে; থাম্বনেল তৈরি হলেই বসে যাবে
        if (thumb.isBlank()) makeThumbnailAsync(video)
        Result.success(video)
    }

    private fun makeThumbnailAsync(video: VideoEntity) {
        scope.launch {
            val source = video.localFilePath?.takeIf { File(it).exists() } ?: video.streamUrl
            val path = ThumbnailGenerator.generate(context, video.id, source)
            if (path != null) videoDao.updateThumbnail(video.id, path)
        }
    }

    /** আগের যেসব ভিডিওর থাম্বনেল নেই, সেগুলোর জন্য একটা একটা করে বানায়। */
    fun backfillThumbnails() {
        scope.launch {
            videoDao.getAllVideosSnapshot()
                .filter { it.thumbnailUrl.isBlank() }
                .forEach { v ->
                    val source = v.localFilePath?.takeIf { File(it).exists() } ?: v.streamUrl
                    val path = ThumbnailGenerator.generate(context, v.id, source)
                    if (path != null) videoDao.updateThumbnail(v.id, path)
                }
        }
    }

    suspend fun toggleFavorite(videoId: String) = withContext(Dispatchers.IO) {
        val video = videoDao.getVideoById(videoId) ?: return@withContext
        videoDao.updateFavorite(videoId, !video.isFavorite)
    }

    suspend fun updatePlaybackPosition(videoId: String, positionMs: Long) = withContext(Dispatchers.IO) {
        videoDao.updatePlaybackPosition(videoId, positionMs)
    }

    suspend fun deleteVideo(video: VideoEntity) = withContext(Dispatchers.IO) {
        // If downloaded file exists, delete it
        video.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        val download = downloadDao.getDownloadByVideoId(video.id)
        if (download != null) {
            downloadDao.deleteDownload(download)
        }
        if (video.thumbnailUrl.startsWith("/")) {
            File(video.thumbnailUrl).delete()
        }
        videoDao.deleteVideo(video)
    }

    // Playlist Management
    suspend fun createPlaylist(name: String): PlaylistEntity = withContext(Dispatchers.IO) {
        val playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            isSystem = false
        )
        playlistDao.insertPlaylist(playlist)
        playlist
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) = withContext(Dispatchers.IO) {
        val current = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        if (!current.isSystem) {
            playlistDao.updatePlaylist(current.copy(name = newName.trim()))
        }
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addVideoToPlaylist(videoId: String, playlistName: String) = withContext(Dispatchers.IO) {
        val video = videoDao.getVideoById(videoId) ?: return@withContext
        val currentList = video.playlistNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        currentList.add(playlistName.trim())
        videoDao.updateVideo(video.copy(playlistNames = currentList.joinToString(",")))
    }

    suspend fun removeVideoFromPlaylist(videoId: String, playlistName: String) = withContext(Dispatchers.IO) {
        val video = videoDao.getVideoById(videoId) ?: return@withContext
        val currentList = video.playlistNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        currentList.remove(playlistName.trim())
        videoDao.updateVideo(video.copy(playlistNames = currentList.joinToString(",")))
    }

    // Download Management
    suspend fun enqueueDownload(video: VideoEntity, toAppPrivate: Boolean): DownloadItem = withContext(Dispatchers.IO) {
        val targetDir = if (toAppPrivate) {
            context.getExternalFilesDir("MyTubeDownloads") ?: context.filesDir
        } else {
            // Accessible app-specific external media
            context.getExternalFilesDir(null) ?: context.filesDir
        }
        targetDir.mkdirs()

        val sanitizedTitle = video.title.replace(Regex("[^a-zA-Z0-9.\\-]"), "_").take(40)
        val file = File(targetDir, "${sanitizedTitle}_${video.id.take(6)}.mp4")

        val existing = downloadDao.getDownloadByVideoId(video.id)
        if (existing != null) {
            return@withContext existing
        }

        val download = DownloadItem(
            id = UUID.randomUUID().toString(),
            videoId = video.id,
            title = video.title,
            downloadUrl = video.streamUrl,
            targetFilePath = file.absolutePath,
            status = DownloadStatus.QUEUED
        )
        downloadDao.insertDownload(download)
        download
    }

    suspend fun cancelDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val item = downloadDao.getDownloadById(downloadId) ?: return@withContext
        val file = File(item.targetFilePath)
        if (file.exists()) {
            file.delete()
        }
        downloadDao.deleteDownloadById(downloadId)
    }

    // Storage and Cache calculation
    suspend fun calculateCacheAndStorage(): StorageBreakdown = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val cacheSize = getFolderSize(cacheDir)

        val downloadsDir = context.getExternalFilesDir("MyTubeDownloads") ?: context.filesDir
        val downloadsSize = getFolderSize(downloadsDir)

        val internalFilesDir = context.filesDir
        val internalSize = getFolderSize(internalFilesDir)

        StorageBreakdown(
            cacheSizeBytes = cacheSize,
            downloadsSizeBytes = downloadsSize,
            appDataSizeBytes = internalSize
        )
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        deleteFolderContents(cacheDir)
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return size
    }

    private fun deleteFolderContents(file: File?) {
        if (file == null || !file.exists()) return
        file.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteFolderContents(child)
            }
            child.delete()
        }
    }
}

data class StorageBreakdown(
    val cacheSizeBytes: Long,
    val downloadsSizeBytes: Long,
    val appDataSizeBytes: Long
) {
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
