package com.example.playme.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
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
            player.setPlaybackSpeed(speed)
        }
    }

    fun getOrCreatePlayer(index: Int, video: VideoEntity): ExoPlayer {
        val existing = playerMap[index]
        if (existing != null) {
            return existing
        }

        val player = ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse(video.uriString)
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE // TikTok style continuous loop
            setPlaybackSpeed(currentSpeed)
            prepare()
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
                entry.value.stop()
                entry.value.release()
                iterator.remove()
            }
        }

        // Ensure active window items are prepared
        for (i in activeWindow) {
            if (i in videos.indices) {
                val player = getOrCreatePlayer(i, videos[i])
                player.setPlaybackSpeed(currentSpeed)
                if (i == currentIndex) {
                    player.playWhenReady = true
                } else {
                    player.playWhenReady = false
                    player.seekTo(0)
                }
            }
        }
    }

    fun releaseAll() {
        for (player in playerMap.values) {
            player.stop()
            player.release()
        }
        playerMap.clear()
    }
}
