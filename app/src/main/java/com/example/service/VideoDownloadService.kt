package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.MyTubeDatabase
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

class VideoDownloadService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var downloadJob: Job? = null

    private lateinit var database: MyTubeDatabase
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val CHANNEL_ID = "mytube_download_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_QUEUE = "com.example.mytube.START_QUEUE"
        const val ACTION_PAUSE = "com.example.mytube.PAUSE"
        const val ACTION_CANCEL = "com.example.mytube.CANCEL"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun start(context: Context) {
            val intent = Intent(context, VideoDownloadService::class.java).apply {
                action = ACTION_START_QUEUE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = MyTubeDatabase.getDatabase(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getStringExtra(EXTRA_DOWNLOAD_ID)

        startForeground(NOTIFICATION_ID, buildNotification("MyTube Downloader", "Queue processing...", 0, 0))

        when (action) {
            ACTION_PAUSE -> {
                if (downloadId != null) {
                    serviceScope.launch {
                        database.downloadDao().updateStatus(downloadId, DownloadStatus.PAUSED)
                        downloadJob?.cancel()
                        processNextInQueue()
                    }
                }
            }
            ACTION_CANCEL -> {
                if (downloadId != null) {
                    serviceScope.launch {
                        val item = database.downloadDao().getDownloadById(downloadId)
                        item?.let {
                            File(it.targetFilePath).delete()
                        }
                        database.downloadDao().deleteDownloadById(downloadId)
                        downloadJob?.cancel()
                        processNextInQueue()
                    }
                }
            }
            else -> {
                processNextInQueue()
            }
        }

        return START_NOT_STICKY
    }

    private fun processNextInQueue() {
        if (downloadJob?.isActive == true) return

        downloadJob = serviceScope.launch {
            val downloadDao = database.downloadDao()
            val videoDao = database.videoDao()

            val queued = downloadDao.getDownloadById("") // dummy check
            val all = database.downloadDao().getDownloadById("")

            // Find next QUEUED or DOWNLOADING item
            // Query database directly
            val allDownloads = database.openHelper.readableDatabase
            val cursor = database.query(
                "SELECT id FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING') ORDER BY createdAt ASC LIMIT 1",
                null
            )
            var nextId: String? = null
            if (cursor.moveToFirst()) {
                nextId = cursor.getString(0)
            }
            cursor.close()

            if (nextId == null) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            val item = downloadDao.getDownloadById(nextId) ?: return@launch
            downloadFile(item)
            // Continue queue
            processNextInQueue()
        }
    }

    private suspend fun downloadFile(item: DownloadItem) {
        val downloadDao = database.downloadDao()
        val videoDao = database.videoDao()

        downloadDao.updateStatus(item.id, DownloadStatus.DOWNLOADING)

        val targetFile = File(item.targetFilePath)
        targetFile.parentFile?.mkdirs()

        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        try {
            val requestBuilder = Request.Builder()
                .url(item.downloadUrl)
                .header("User-Agent", "Mozilla/5.0 MyTube Downloader")

            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                downloadDao.updateStatus(item.id, DownloadStatus.FAILED, "HTTP ${response.code}")
                return
            }

            val body = response.body ?: run {
                downloadDao.updateStatus(item.id, DownloadStatus.FAILED, "Empty response body")
                return
            }

            val contentLength = body.contentLength()
            val totalBytes = if (response.code == 206) {
                existingBytes + contentLength
            } else {
                contentLength
            }

            val outputStream = if (response.code == 206 && existingBytes > 0) {
                val raf = RandomAccessFile(targetFile, "rw")
                raf.seek(existingBytes)
                FileOutputStream(raf.fd)
            } else {
                FileOutputStream(targetFile, false)
            }

            val buffer = ByteArray(64 * 1024)
            var downloaded = if (response.code == 206) existingBytes else 0L
            val inputStream = body.byteStream()

            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            outputStream.use { out ->
                inputStream.use { input ->
                    while (serviceScope.isActive) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        out.write(buffer, 0, read)
                        downloaded += read
                        bytesSinceLastUpdate += read

                        val currentTime = System.currentTimeMillis()
                        val diff = currentTime - lastUpdateTime
                        if (diff >= 1000) {
                            val speedBps = (bytesSinceLastUpdate * 1000) / diff
                            val remainingBytes = if (totalBytes > downloaded) totalBytes - downloaded else 0L
                            val eta = if (speedBps > 0) remainingBytes / speedBps else 0L

                            downloadDao.updateProgress(item.id, downloaded, totalBytes, speedBps, eta)

                            val progress = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                            val speedStr = String.format("%.1f KB/s", speedBps / 1024.0)

                            val notification = buildNotification(
                                title = "Downloading: ${item.title}",
                                content = "$progress% • $speedStr",
                                progress = progress,
                                max = 100
                            )
                            val manager = getSystemService(NotificationManager::class.java)
                            manager.notify(NOTIFICATION_ID, notification)

                            bytesSinceLastUpdate = 0L
                            lastUpdateTime = currentTime
                        }
                    }
                }
            }

            if (totalBytes > 0 && downloaded >= totalBytes) {
                downloadDao.updateStatus(item.id, DownloadStatus.COMPLETED)
                videoDao.updateDownloadStatus(item.videoId, true, targetFile.absolutePath)

                val completeNotification = buildNotification(
                    title = "Download Complete!",
                    content = item.title,
                    progress = 100,
                    max = 100
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, completeNotification)
            } else {
                downloadDao.updateProgress(item.id, downloaded, totalBytes, 0, 0)
            }
        } catch (e: Exception) {
            downloadDao.updateStatus(item.id, DownloadStatus.FAILED, e.localizedMessage)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MyTube Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active background video download progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String, progress: Int, max: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(progress < max)
            .setOnlyAlertOnce(true)

        if (max > 0) {
            builder.setProgress(max, progress, false)
        }

        return builder.build()
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
