package com.example.playme.ui.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.playme.data.db.VideoEntity

@OptIn(UnstableApi::class)
class ExoPlayerPool(private val context: Context) {

    // Keyed by videoId (Long) so that list reordering preserves players seamlessly
    private val playerMap = mutableMapOf<Long, ExoPlayer>()
    private var currentSpeed: Float = 1.0f

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        for (player in playerMap.values) {
            try {
                player.setPlaybackSpeed(speed)
            } catch (e: Exception) {
                Log.w("ExoPlayerPool", "Failed to set playback speed", e)
            }
        }
    }

    fun pauseAll() {
        for (player in playerMap.values) {
            try {
                player.pause()
            } catch (e: Exception) {
                Log.w("ExoPlayerPool", "Failed to pause player", e)
            }
        }
    }

    fun getOrCreatePlayer(video: VideoEntity): ExoPlayer {
        val existing = playerMap[video.id]
        if (existing != null) {
            return existing
        }

        val player = ExoPlayer.Builder(context).build().apply {
            try {
                val uri = Uri.parse(video.uriString)
                val mediaItem = MediaItem.fromUri(uri)
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE // TikTok style continuous loop
                setPlaybackSpeed(currentSpeed)
                prepare()
            } catch (e: Exception) {
                Log.e("ExoPlayerPool", "Error preparing player for video ${video.id}", e)
            }

            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("ExoPlayerPool", "ExoPlayer error on video ${video.id}: ${error.errorCodeName}", error)
                }
            })
        }

        playerMap[video.id] = player
        return player
    }

    fun getOrCreatePlayer(index: Int, video: VideoEntity): ExoPlayer {
        return getOrCreatePlayer(video)
    }

    fun updateActiveWindow(currentIndex: Int, videos: List<VideoEntity>) {
        if (videos.isEmpty() || currentIndex !in videos.indices) return

        val activeIndices = setOf(currentIndex - 1, currentIndex, currentIndex + 1)
        val activeVideos = activeIndices.mapNotNull { videos.getOrNull(it) }
        val activeVideoIds = activeVideos.map { it.id }.toSet()

        // Release any player outside the window of 3 active videos
        val iterator = playerMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in activeVideoIds) {
                try {
                    entry.value.stop()
                    entry.value.release()
                } catch (e: Exception) {
                    Log.w("ExoPlayerPool", "Error releasing player", e)
                }
                iterator.remove()
            }
        }

        // Ensure active window items are prepared
        val currentVideo = videos[currentIndex]
        for (video in activeVideos) {
            try {
                val player = getOrCreatePlayer(video)
                player.setPlaybackSpeed(currentSpeed)
                if (video.id == currentVideo.id) {
                    player.playWhenReady = true
                } else {
                    player.playWhenReady = false
                    player.seekTo(0)
                }
            } catch (e: Exception) {
                Log.e("ExoPlayerPool", "Error updating window player for video ${video.id}", e)
            }
        }
    }

    fun releasePlayer(videoId: Long) {
        val player = playerMap.remove(videoId)
        try {
            player?.stop()
            player?.release()
        } catch (e: Exception) {
            Log.w("ExoPlayerPool", "Error releasing player for video $videoId", e)
        }
    }

    fun releaseAll() {
        for (player in playerMap.values) {
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {
                Log.w("ExoPlayerPool", "Error during releaseAll", e)
            }
        }
        playerMap.clear()
    }
}
