package com.example.data.repository

import com.example.data.local.PlaylistDao
import com.example.data.local.VideoDao
import com.example.data.model.PlaylistEntity
import com.example.data.model.VideoEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MyTubeBackupPayload(
    val appVersion: String = "1.0",
    val backupDate: String,
    val timestamp: Long = System.currentTimeMillis(),
    val videos: List<VideoEntity>,
    val playlists: List<PlaylistEntity>
)

sealed class SyncResult {
    data class Success(val message: String, val videoCount: Int, val playlistCount: Int) : SyncResult()
    data class Error(val error: String) : SyncResult()
}

class TelegramSyncRepository(
    private val videoDao: VideoDao,
    private val playlistDao: PlaylistDao,
    private val settingsDataStore: SettingsDataStore,
    private val client: OkHttpClient = OkHttpClient()
) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(MyTubeBackupPayload::class.java)

    suspend fun createExportJson(): String = withContext(Dispatchers.IO) {
        val videos = videoDao.getAllVideosSnapshot()
        val playlists = playlistDao.getAllPlaylistsSnapshot()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val payload = MyTubeBackupPayload(
            backupDate = dateFormat.format(Date()),
            videos = videos,
            playlists = playlists
        )
        adapter.toJson(payload)
    }

    suspend fun importFromJson(jsonString: String): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val payload = adapter.fromJson(jsonString)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid JSON format"))

            var importedVideos = 0
            var importedPlaylists = 0

            payload.playlists.forEach { playlist ->
                playlistDao.insertPlaylist(playlist)
                importedPlaylists++
            }

            payload.videos.forEach { video ->
                val existing = videoDao.getVideoById(video.id)
                if (existing == null) {
                    videoDao.insertVideo(video)
                    importedVideos++
                }
            }

            Result.success(Pair(importedVideos, importedPlaylists))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun backupToTelegram(botToken: String, channelId: String): SyncResult = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || channelId.isBlank()) {
            return@withContext SyncResult.Error("Bot Token এবং Channel ID দিতে হবে!")
        }

        try {
            val jsonContent = createExportJson()
            val videos = videoDao.getAllVideosSnapshot()
            val playlists = playlistDao.getAllPlaylistsSnapshot()

            val fileName = "mytube_backup_${System.currentTimeMillis()}.json"
            val fileBody = jsonContent.toByteArray()
                .toRequestBody("application/json".toMediaType())

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", channelId.trim())
                .addFormDataPart(
                    "caption",
                    "📺 #MyTube Cloud Backup\n📅 Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n🎬 Videos: ${videos.size}\n📂 Playlists: ${playlists.size}"
                )
                .addFormDataPart("document", fileName, fileBody)
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot${botToken.trim()}/sendDocument")
                .post(multipart)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.contains("\"ok\":true")) {
                settingsDataStore.updateLastSync(System.currentTimeMillis())
                SyncResult.Success(
                    message = "Telegram Channel-এ সফলভাবে ব্যাকআপ সংরক্ষিত হয়েছে!",
                    videoCount = videos.size,
                    playlistCount = playlists.size
                )
            } else {
                SyncResult.Error("Telegram API Error: ${response.code} - $responseBody")
            }
        } catch (e: Exception) {
            SyncResult.Error("নেটওয়ার্ক ত্রুটি: ${e.localizedMessage}")
        }
    }
}
