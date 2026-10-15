package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val originalUrl: String,
    val streamUrl: String,
    val thumbnailUrl: String,
    val durationMs: Long = 0L,
    val sourceDomain: String = "",
    val addedTimestamp: Long = System.currentTimeMillis(),
    val lastPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val playlistNames: String = "" // Comma-separated names of playlists it belongs to
)
