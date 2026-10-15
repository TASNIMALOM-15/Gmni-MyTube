package com.example.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ResolvedVideoInfo(
    val title: String,
    val streamUrl: String,
    val originalUrl: String,
    val thumbnailUrl: String,
    val domain: String,
    val durationMs: Long = 0L,
    val isDirectStream: Boolean = true
)

class LinkResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun resolve(rawUrl: String): Result<ResolvedVideoInfo> = withContext(Dispatchers.IO) {
        try {
            val url = rawUrl.trim()
            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid URL format. Must start with http:// or https://"))
            }

            val uri = URI(url)
            val host = uri.host?.lowercase() ?: "Unknown"

            // Handle TeraBox links
            if (isTeraBoxUrl(host)) {
                return@withContext resolveTeraBox(url, host)
            }

            // Normal direct or web URL
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Infinix Smart 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type")?.lowercase() ?: ""

            var title = ""
            var thumbUrl = ""

            val isDirectMedia = contentType.contains("video") ||
                    contentType.contains("application/vnd.apple.mpegurl") ||
                    contentType.contains("application/x-mpegurl") ||
                    finalUrl.endsWith(".mp4", ignoreCase = true) ||
                    finalUrl.endsWith(".mkv", ignoreCase = true) ||
                    finalUrl.endsWith(".webm", ignoreCase = true) ||
                    finalUrl.endsWith(".m3u8", ignoreCase = true) ||
                    finalUrl.endsWith(".mpd", ignoreCase = true)

            if (!isDirectMedia && (contentType.contains("text/html") || contentType.isEmpty())) {
                val html = response.body?.string() ?: ""
                title = extractMetaTag(html, "og:title")
                    ?: extractTitleFromHtml(html)
                    ?: extractNameFromPath(url)
                thumbUrl = extractMetaTag(html, "og:image") ?: ""
            } else {
                response.close()
                title = extractNameFromPath(finalUrl)
            }

            if (title.isBlank()) {
                title = "Video_${UUID.randomUUID().toString().take(6)}"
            }

            Result.success(
                ResolvedVideoInfo(
                    title = title,
                    streamUrl = finalUrl,
                    originalUrl = url,
                    thumbnailUrl = thumbUrl,
                    domain = host,
                    isDirectStream = isDirectMedia
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isTeraBoxUrl(host: String): Boolean {
        return host.contains("terabox") ||
                host.contains("1024tera") ||
                host.contains("teraboxapp") ||
                host.contains("mirrobox") ||
                host.contains("nephobox")
    }

    private fun resolveTeraBox(url: String, host: String): Result<ResolvedVideoInfo> {
        // TeraBox links require either user session cookies or a bot bridge (e.g., the Clode TG bot).
        // If passed directly, we save metadata and instruct user that direct streaming will connect via Bot bridge if configured.
        val title = "TeraBox Video [${extractNameFromPath(url)}]"
        return Result.success(
            ResolvedVideoInfo(
                title = title,
                streamUrl = url,
                originalUrl = url,
                thumbnailUrl = "",
                domain = host,
                isDirectStream = false
            )
        )
    }

    private fun extractMetaTag(html: String, property: String): String? {
        val pattern = Pattern.compile("<meta[^>]+property=[\"']$property[\"'][^>]+content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        val altPattern = Pattern.compile("<meta[^>]+content=[\"'](.*?)[\"'][^>]+property=[\"']$property[\"']", Pattern.CASE_INSENSITIVE)
        val altMatcher = altPattern.matcher(html)
        if (altMatcher.find()) {
            return altMatcher.group(1)?.trim()
        }
        return null
    }

    private fun extractTitleFromHtml(html: String): String? {
        val pattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun extractNameFromPath(url: String): String {
        return try {
            val path = URI(url).path
            val filename = path.substringAfterLast('/')
            if (filename.isNotBlank()) {
                val clean = filename.substringBeforeLast('.')
                    .replace(Regex("[_\\-+]"), " ")
                    .trim()
                if (clean.isNotBlank()) clean else "MyTube Video"
            } else "MyTube Video"
        } catch (_: Exception) {
            "MyTube Video"
        }
    }
}
