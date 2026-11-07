package com.example.playme.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class PlaylistCountTuple(
    val playlistId: Int,
    val count: Int
)

@Dao
interface PlayMeDao {

    @Query("SELECT * FROM videos ORDER BY id ASC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE uriString = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>): List<Long>

    @Query("UPDATE videos SET lastPositionMs = :positionMs WHERE id = :id")
    suspend fun updateLastPosition(id: Long, positionMs: Long)

    @Query("SELECT COUNT(*) FROM videos")
    fun getVideoCountFlow(): Flow<Int>

    @Query("SELECT SUM(fileSizeBytes) FROM videos")
    fun getTotalSizeBytesFlow(): Flow<Long?>

    @Query("DELETE FROM videos")
    suspend fun clearAllVideos()

    @Query("DELETE FROM videos WHERE uriString NOT IN (:existingUris)")
    suspend fun deleteStaleVideos(existingUris: List<String>)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY id ASC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("SELECT videos.* FROM videos INNER JOIN video_playlist_cross_ref ON videos.id = video_playlist_cross_ref.videoId WHERE video_playlist_cross_ref.playlistId = :playlistId ORDER BY video_playlist_cross_ref.addedAt DESC")
    fun getVideosForPlaylistFlow(playlistId: Int): Flow<List<VideoEntity>>

    @Query("SELECT playlistId, COUNT(videoId) as count FROM video_playlist_cross_ref GROUP BY playlistId")
    fun getPlaylistCountsFlow(): Flow<List<PlaylistCountTuple>>

    @Query("SELECT playlistId FROM video_playlist_cross_ref WHERE videoId = :videoId")
    fun getPlaylistIdsForVideoFlow(videoId: Long): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVideoToPlaylist(ref: VideoPlaylistCrossRef)

    @Query("DELETE FROM video_playlist_cross_ref WHERE videoId = :videoId AND playlistId = :playlistId")
    suspend fun removeVideoFromPlaylist(videoId: Long, playlistId: Int)

    @Query("SELECT * FROM video_playlist_cross_ref")
    fun getAllCrossRefsFlow(): Flow<List<VideoPlaylistCrossRef>>

    @Query("SELECT EXISTS(SELECT 1 FROM video_playlist_cross_ref WHERE videoId = :videoId AND playlistId = :playlistId)")
    suspend fun isVideoInPlaylist(videoId: Long, playlistId: Int): Boolean

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)

    @Query("DELETE FROM video_playlist_cross_ref WHERE videoId = :videoId")
    suspend fun deleteCrossRefsForVideo(videoId: Long)

    // App Settings
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)
}
