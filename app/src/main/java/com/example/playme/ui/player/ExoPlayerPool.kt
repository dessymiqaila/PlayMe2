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

    private val playerMap = mutableMapOf<Int, ExoPlayer>()
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

    fun getOrCreatePlayer(index: Int, video: VideoEntity): ExoPlayer {
        val existing = playerMap[index]
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
                Log.e("ExoPlayerPool", "Error preparing player for index $index", e)
            }

            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("ExoPlayerPool", "ExoPlayer error on video index $index: ${error.errorCodeName}", error)
                }
            })
        }

        playerMap[index] = player
        return player
    }

    fun updateActiveWindow(currentIndex: Int, videos: List<VideoEntity>) {
        val activeWindow = setOf(currentIndex - 1, currentIndex, currentIndex + 1)

        // Release any player outside the window [current - 1, current, current + 1]
        val iterator = playerMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in activeWindow) {
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
        for (i in activeWindow) {
            if (i in videos.indices) {
                try {
                    val player = getOrCreatePlayer(i, videos[i])
                    player.setPlaybackSpeed(currentSpeed)
                    if (i == currentIndex) {
                        player.playWhenReady = true
                    } else {
                        player.playWhenReady = false
                        player.seekTo(0)
                    }
                } catch (e: Exception) {
                    Log.e("ExoPlayerPool", "Error updating window player for index $i", e)
                }
            }
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
