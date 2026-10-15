package com.example

import android.app.Application
import com.example.data.local.MyTubeDatabase
import com.example.data.repository.SettingsDataStore
import com.example.data.repository.TelegramSyncRepository
import com.example.data.repository.VideoRepository

class MyTubeApplication : Application() {

    lateinit var database: MyTubeDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var videoRepository: VideoRepository
        private set

    lateinit var telegramSyncRepository: TelegramSyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = MyTubeDatabase.getDatabase(this)
        settingsDataStore = SettingsDataStore(this)
        videoRepository = VideoRepository(
            context = this,
            videoDao = database.videoDao(),
            playlistDao = database.playlistDao(),
            downloadDao = database.downloadDao()
        )
        videoRepository.backfillThumbnails()
        telegramSyncRepository = TelegramSyncRepository(
            videoDao = database.videoDao(),
            playlistDao = database.playlistDao(),
            settingsDataStore = settingsDataStore
        )
    }
}
