package com.example.playme.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.playme.data.db.AppSettingEntity
import com.example.playme.data.db.PlayMeDao
import com.example.playme.data.db.PlaylistEntity
import com.example.playme.data.db.VideoEntity
import com.example.playme.data.db.VideoPlaylistCrossRef
import com.example.playme.data.scanner.FolderScanner
import com.example.playme.data.scanner.SampleVideoGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class PlayMeRepository(private val dao: PlayMeDao) {

    val allVideos: Flow<List<VideoEntity>> = dao.getAllVideos()
    val videoCount: Flow<Int> = dao.getVideoCountFlow()
    val totalSizeBytes: Flow<Long?> = dao.getTotalSizeBytesFlow()
    val allPlaylists: Flow<List<PlaylistEntity>> = dao.getAllPlaylistsFlow()

    val playlistCounts: Flow<Map<Int, Int>> = dao.getPlaylistCountsFlow().map { list ->
        list.associate { it.playlistId to it.count }
    }

    val videoPlaylistMap: Flow<Map<Long, Set<Int>>> = dao.getAllCrossRefsFlow().map { list ->
        list.groupBy({ it.videoId }, { it.playlistId })
            .mapValues { it.value.toSet() }
    }

    fun getVideosForPlaylist(playlistId: Int): Flow<List<VideoEntity>> {
        return dao.getVideosForPlaylistFlow(playlistId)
    }

    /**
     * Deletes the video both from the database and physically from disk/SAF storage.
     */
    suspend fun deleteVideo(context: Context, videoId: Long): Boolean = withContext(Dispatchers.IO) {
        val video = dao.getVideoById(videoId)
        var fileDeleted = false
        if (video != null) {
            val uriStr = video.uriString
            try {
                val uri = Uri.parse(uriStr)
                if (uri.scheme == "content") {
                    try {
                        val docFile = DocumentFile.fromSingleUri(context, uri)
                        if (docFile != null && docFile.exists()) {
                            fileDeleted = docFile.delete()
                        }
                    } catch (e: Exception) {
                        Log.w("PlayMeRepository", "DocumentFile delete failed, trying fallback", e)
                    }
                    if (!fileDeleted) {
                        try {
                            fileDeleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
                        } catch (e: Exception) {
                            val count = context.contentResolver.delete(uri, null, null)
                            fileDeleted = count > 0
                        }
                    }
                } else {
                    val filePath = if (uri.scheme == "file") uri.path ?: "" else uriStr
                    val file = File(filePath)
                    if (file.exists()) {
                        fileDeleted = file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayMeRepository", "Failed to physically delete file for video $videoId: $uriStr", e)
            }
        }
        // Remove associations and video row from Room
        dao.deleteCrossRefsForVideo(videoId)
        dao.deleteVideoById(videoId)
        fileDeleted
    }

    suspend fun deleteVideo(videoId: Long) = withContext(Dispatchers.IO) {
        dao.deleteCrossRefsForVideo(videoId)
        dao.deleteVideoById(videoId)
    }

    fun getPlaylistIdsForVideo(videoId: Long): Flow<List<Int>> {
        return dao.getPlaylistIdsForVideoFlow(videoId)
    }

    suspend fun toggleVideoInPlaylist(videoId: Long, playlistId: Int): Boolean = withContext(Dispatchers.IO) {
        val exists = dao.isVideoInPlaylist(videoId, playlistId)
        if (exists) {
            dao.removeVideoFromPlaylist(videoId, playlistId)
            false
        } else {
            dao.addVideoToPlaylist(VideoPlaylistCrossRef(videoId = videoId, playlistId = playlistId))
            true
        }
    }

    suspend fun isVideoInPlaylist(videoId: Long, playlistId: Int): Boolean = withContext(Dispatchers.IO) {
        dao.isVideoInPlaylist(videoId, playlistId)
    }

    suspend fun updateLastPosition(videoId: Long, positionMs: Long) = withContext(Dispatchers.IO) {
        dao.updateLastPosition(videoId, positionMs)
    }

    fun getSettingFlow(key: String): Flow<String?> = dao.getSettingFlow(key)

    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
        dao.getSetting(key)
    }

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        dao.setSetting(AppSettingEntity(key, value))
    }

    suspend fun scanFolder(context: Context, treeUri: Uri): FolderScanner.ScanResult = withContext(Dispatchers.IO) {
        val result = FolderScanner.scanFolderUri(context, treeUri)
        if (result.videos.isNotEmpty()) {
            dao.insertVideos(result.videos)
            val currentUris = result.videos.map { it.uriString }
            dao.deleteStaleVideos(currentUris)
        }
        saveSetting("active_folder_uri", treeUri.toString())
        saveSetting("active_folder_display", result.folderDisplayName)
        saveSetting("last_scan_time", result.scanTimestamp)
        saveSetting("total_bytes_str", FolderScanner.formatFileSize(result.totalBytes))
        result
    }

    suspend fun seedDemoVideos(context: Context): List<VideoEntity> = withContext(Dispatchers.IO) {
        val demoVideos = SampleVideoGenerator.generateOrGetDemoVideos(context)
        if (demoVideos.isNotEmpty()) {
            dao.insertVideos(demoVideos)
            saveSetting("active_folder_uri", "demo://local_samples")
            saveSetting("active_folder_display", "Demo Video Clips (PlayMe Sample)")
            saveSetting("last_scan_time", FolderScanner.formatTimestamp(System.currentTimeMillis()))
            val totalBytes = demoVideos.sumOf { it.fileSizeBytes }
            saveSetting("total_bytes_str", FolderScanner.formatFileSize(totalBytes))

            val v1 = dao.getVideoByUri(demoVideos.getOrNull(0)?.uriString ?: "")
            val v2 = dao.getVideoByUri(demoVideos.getOrNull(1)?.uriString ?: "")
            val v3 = dao.getVideoByUri(demoVideos.getOrNull(2)?.uriString ?: "")

            if (v1 != null) {
                dao.addVideoToPlaylist(VideoPlaylistCrossRef(v1.id, 1))
                dao.addVideoToPlaylist(VideoPlaylistCrossRef(v1.id, 2)) // Multi-playlist!
            }
            if (v2 != null) {
                dao.addVideoToPlaylist(VideoPlaylistCrossRef(v2.id, 2))
                dao.addVideoToPlaylist(VideoPlaylistCrossRef(v2.id, 3))
            }
            if (v3 != null) {
                dao.addVideoToPlaylist(VideoPlaylistCrossRef(v3.id, 6))
            }
        }
        demoVideos
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAllVideos()
        saveSetting("active_folder_uri", "")
        saveSetting("active_folder_display", "No folder selected")
        saveSetting("last_scan_time", "Never scanned")
        saveSetting("total_bytes_str", "0 B")
    }
}
