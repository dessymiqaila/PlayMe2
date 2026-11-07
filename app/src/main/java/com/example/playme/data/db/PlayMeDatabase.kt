package com.example.playme.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        VideoPlaylistCrossRef::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PlayMeDatabase : RoomDatabase() {
    abstract fun playMeDao(): PlayMeDao

    companion object {
        @Volatile
        private var INSTANCE: PlayMeDatabase? = null

        fun getDatabase(context: Context): PlayMeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlayMeDatabase::class.java,
                    "playme_video_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed playlists 1..5
                            CoroutineScope(Dispatchers.IO).launch {
                                val seeded = (1..5).map {
                                    PlaylistEntity(id = it, name = "Playlist $it")
                                }
                                INSTANCE?.playMeDao()?.insertPlaylists(seeded)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
