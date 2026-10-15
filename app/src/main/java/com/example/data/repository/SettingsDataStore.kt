package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mytube_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val KEY_AUTO_PLAY = booleanPreferencesKey("auto_play_next")
        val KEY_APP_LOCK = booleanPreferencesKey("is_app_lock_enabled")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_AMOLED = booleanPreferencesKey("is_amoled_black")
        val KEY_GRID_LAYOUT = booleanPreferencesKey("is_grid_layout")
        val KEY_DOWNLOAD_PRIVATE = booleanPreferencesKey("download_to_app_private")
        val KEY_TG_TOKEN = stringPreferencesKey("telegram_bot_token")
        val KEY_TG_CHANNEL = stringPreferencesKey("telegram_channel_id")
        val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync_on_open")
        val KEY_LAST_SYNC = longPreferencesKey("last_sync_timestamp")

        fun hashPin(pin: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(pin.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            defaultPlaybackSpeed = preferences[KEY_PLAYBACK_SPEED] ?: 1.0f,
            autoPlayNext = preferences[KEY_AUTO_PLAY] ?: true,
            isAppLockEnabled = preferences[KEY_APP_LOCK] ?: false,
            pinHash = preferences[KEY_PIN_HASH] ?: "",
            isDarkTheme = preferences[KEY_DARK_THEME] ?: true,
            isAmoledBlack = preferences[KEY_AMOLED] ?: false,
            isGridLayout = preferences[KEY_GRID_LAYOUT] ?: true,
            downloadToAppPrivate = preferences[KEY_DOWNLOAD_PRIVATE] ?: true,
            telegramBotToken = preferences[KEY_TG_TOKEN] ?: "",
            telegramChannelId = preferences[KEY_TG_CHANNEL] ?: "",
            autoSyncOnOpen = preferences[KEY_AUTO_SYNC] ?: false,
            lastSyncTimestamp = preferences[KEY_LAST_SYNC] ?: 0L
        )
    }

    suspend fun updatePlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[KEY_PLAYBACK_SPEED] = speed }
    }

    suspend fun updateAutoPlay(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_PLAY] = enabled }
    }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit {
            it[KEY_PIN_HASH] = hash
            it[KEY_APP_LOCK] = true
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit {
            it[KEY_APP_LOCK] = false
            it[KEY_PIN_HASH] = ""
        }
    }

    suspend fun updateTheme(isDark: Boolean, isAmoled: Boolean) {
        context.dataStore.edit {
            it[KEY_DARK_THEME] = isDark
            it[KEY_AMOLED] = isAmoled
        }
    }

    suspend fun updateLayout(isGrid: Boolean) {
        context.dataStore.edit { it[KEY_GRID_LAYOUT] = isGrid }
    }

    suspend fun updateDownloadLocation(toAppPrivate: Boolean) {
        context.dataStore.edit { it[KEY_DOWNLOAD_PRIVATE] = toAppPrivate }
    }

    suspend fun updateTelegramConfig(token: String, channelId: String, autoSync: Boolean) {
        context.dataStore.edit {
            it[KEY_TG_TOKEN] = token
            it[KEY_TG_CHANNEL] = channelId
            it[KEY_AUTO_SYNC] = autoSync
        }
    }

    suspend fun updateLastSync(timestamp: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC] = timestamp }
    }
}
