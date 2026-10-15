package com.example.data.model

data class AppSettings(
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoPlayNext: Boolean = true,
    val isAppLockEnabled: Boolean = false,
    val pinHash: String = "",
    val isDarkTheme: Boolean = true,
    val isAmoledBlack: Boolean = false,
    val isGridLayout: Boolean = true,
    val downloadToAppPrivate: Boolean = true, // When true, uses Android/data/.../files/Downloads
    val telegramBotToken: String = "",
    val telegramChannelId: String = "",
    val autoSyncOnOpen: Boolean = false,
    val lastSyncTimestamp: Long = 0L
)
