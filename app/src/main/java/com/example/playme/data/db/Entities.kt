package com.example.playme.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [Index(value = ["uriString"], unique = true)]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val dateModified: Long,
    val lastPositionMs: Long = 0L
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: Int, // 1 to 5
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "video_playlist_cross_ref",
    primaryKeys = ["videoId", "playlistId"],
    indices = [Index(value = ["playlistId"]), Index(value = ["videoId"])]
)
data class VideoPlaylistCrossRef(
    val videoId: Long,
    val playlistId: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
