package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

@OptIn(UnstableApi::class)
object ExoPlayerHelper {

    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "exoplayer_media_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024) // 200MB media cache
            val databaseProvider = StandaloneDatabaseProvider(context)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    fun createPlayer(context: Context, defaultSpeed: Float = 1.0f): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) MyTube")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val cache = getCache(context)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        // Preload buffer configuration: fast startup with 10-15s buffer
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs (15s)
                30000, // maxBufferMs (30s)
                1000,  // bufferForPlaybackMs (1s fast start)
                2500   // bufferForPlaybackAfterRebufferMs (2.5s)
            )
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                playbackParameters = PlaybackParameters(defaultSpeed)
            }
    }

    fun buildMediaItem(url: String, title: String): MediaItem {
        val uri = android.net.Uri.parse(url)
        val builder = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(url)

        val path = uri.path?.lowercase() ?: ""
        when {
            path.endsWith(".m3u8") -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            path.endsWith(".mpd") -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            path.endsWith(".mp4") -> builder.setMimeType(MimeTypes.VIDEO_MP4)
            path.endsWith(".webm") -> builder.setMimeType(MimeTypes.VIDEO_WEBM)
            path.endsWith(".mkv") -> builder.setMimeType(MimeTypes.VIDEO_MATROSKA)
        }

        return builder.build()
    }
}
