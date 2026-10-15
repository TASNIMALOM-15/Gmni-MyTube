package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DownloadItem
import com.example.data.model.PlaylistEntity
import com.example.data.model.VideoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [VideoEntity::class, PlaylistEntity::class, DownloadItem::class],
    version = 1,
    exportSchema = false
)
abstract class MyTubeDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: MyTubeDatabase? = null

        fun getDatabase(context: Context): MyTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyTubeDatabase::class.java,
                    "mytube_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate default playlists
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).playlistDao()
                                dao.insertPlaylists(
                                    listOf(
                                        PlaylistEntity(
                                            id = "default_favorites",
                                            name = "Favorites",
                                            isSystem = true
                                        ),
                                        PlaylistEntity(
                                            id = "default_watch_later",
                                            name = "Watch Later",
                                            isSystem = false
                                        )
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
