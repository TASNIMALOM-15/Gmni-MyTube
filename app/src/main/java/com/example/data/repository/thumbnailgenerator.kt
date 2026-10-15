package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream

/** ভিডিওর মাঝের অংশ থেকে একটা ছবি (thumbnail) বানায়। */
object ThumbnailGenerator {

    private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

    /** সফল হলে ছবির ফাইল-পাথ ফেরত দেয়, না হলে null। */
    suspend fun generate(context: Context, id: String, source: String): String? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            val job = CoroutineScope(Dispatchers.IO).async {
                try {
                    if (source.startsWith("http", ignoreCase = true)) {
                        retriever.setDataSource(source, mapOf("User-Agent" to UA))
                    } else {
                        retriever.setDataSource(source)
                    }
                    val durationMs = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    // ঠিক মাঝখান (50%)
                    val timeUs = if (durationMs > 0) (durationMs / 2) * 1000 else 0L

                    var bmp: Bitmap? = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: retriever.getFrameAtTime(0L)
                    if (bmp == null) return@async null

                    // ছোট করে নেওয়া (দ্রুত লোড + কম জায়গা)
                    val maxW = 640
                    if (bmp.width > maxW) {
                        val h = (bmp.height * (maxW.toFloat() / bmp.width)).toInt()
                        bmp = Bitmap.createScaledBitmap(bmp, maxW, h, true)
                    }

                    val dir = File(context.filesDir, "thumbs").apply { mkdirs() }
                    val file = File(dir, "$id.jpg")
                    val finalBmp: Bitmap = bmp
                    FileOutputStream(file).use { finalBmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                    file.absolutePath
                } catch (e: Throwable) {
                    null
                } finally {
                    try { retriever.release() } catch (_: Throwable) {}
                }
            }
            // ২৫ সেকেন্ডের মধ্যে না হলে হাল ছেড়ে দেবে
            withTimeoutOrNull(25_000) { job.await() }
        }
}
