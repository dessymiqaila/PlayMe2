package com.example.playme.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.playme.data.db.VideoEntity
import com.example.playme.data.model.PlayOrderMode
import com.example.playme.ui.components.OsdOverlay
import com.example.playme.ui.components.PlayPausePulseOverlay
import com.example.playme.ui.components.VideoProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VerticalPlayerScreen(
    videos: List<VideoEntity>,
    initialIndex: Int,
    playMode: PlayOrderMode,
    playbackSpeed: Float = 1.0f,
    activePlaylistIdsForCurrentVideo: Set<Int>,
    onCyclePlayMode: () -> Unit,
    onSpeedChanged: (Float) -> Unit = {},
    onTogglePlaylist: (videoId: Long, playlistId: Int) -> Unit,
    onVideoChanged: (video: VideoEntity, index: Int) -> Unit,
    onSavePosition: (videoId: Long, positionMs: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No videos to play", color = Color.White, fontSize = 16.sp)
        }
        BackHandler { onBack() }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safeInitialIndex = initialIndex.coerceIn(0, videos.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { videos.size }
    )

    // ExoPlayer window pool (manages current, prev, next)
    val playerPool = remember { ExoPlayerPool(context) }
    DisposableEffect(Unit) {
        onDispose {
            playerPool.releaseAll()
        }
    }

    // Default: hidden OSD and playlist stack
    var isOsdVisible by remember { mutableStateOf(false) }
    var pulseToken by remember { mutableLongStateOf(0L) }
    var isPlayingState by remember { mutableStateOf(true) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var currentDurationMs by remember { mutableLongStateOf(0L) }

    val currentPage = pagerState.currentPage
    val activeVideo = videos.getOrNull(currentPage)

    // Auto-hide OSD when pager scrolls/changes page
    LaunchedEffect(pagerState.currentPage) {
        isOsdVisible = false
        if (activeVideo != null) {
            onVideoChanged(activeVideo, currentPage)
        }
    }

    // Auto-hide OSD after 4 seconds of inactivity if visible
    LaunchedEffect(isOsdVisible) {
        if (isOsdVisible) {
            delay(4000)
            isOsdVisible = false
        }
    }

    // Update player pool playback speed
    LaunchedEffect(playbackSpeed) {
        playerPool.setPlaybackSpeed(playbackSpeed)
    }

    // Keep active player window updated and sync loop/play state
    LaunchedEffect(currentPage, videos, playMode) {
        playerPool.updateActiveWindow(currentPage, videos)
        val activePlayer = playerPool.getOrCreatePlayer(currentPage, videos[currentPage])

        activePlayer.repeatMode = Player.REPEAT_MODE_ONE // TikTok style continuous loop
        activePlayer.play()
        isPlayingState = true
    }

    // Coroutine to poll playback position and duration for seekbar
    LaunchedEffect(currentPage, videos) {
        while (isActive) {
            if (currentPage in videos.indices) {
                val player = playerPool.getOrCreatePlayer(currentPage, videos[currentPage])
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                currentDurationMs = player.duration.coerceAtLeast(0L)
                isPlayingState = player.isPlaying
            }
            delay(150)
        }
    }

    // Function to handle play/pause toggle
    val togglePlayPause = {
        if (currentPage in videos.indices) {
            val player = playerPool.getOrCreatePlayer(currentPage, videos[currentPage])
            if (player.isPlaying) {
                player.pause()
                isPlayingState = false
            } else {
                player.play()
                isPlayingState = true
            }
            pulseToken = System.currentTimeMillis()
            // Clicking/double-clicking shows OSD & playlist as requested
            isOsdVisible = true
        }
    }

    // Save position periodically and on back
    BackHandler {
        if (activeVideo != null && currentPage in videos.indices) {
            val player = playerPool.getOrCreatePlayer(currentPage, activeVideo)
            onSavePosition(activeVideo.id, player.currentPosition)
        }
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("vertical_player_screen")
    ) {
        // Horizontal drag swipe accumulator
        var horizontalDragX by remember { mutableFloatStateOf(0f) }

        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val video = videos[page]
            val player = playerPool.getOrCreatePlayer(page, video)

            Box(modifier = Modifier.fillMaxSize()) {
                // Video Surface
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setOnTouchListener { _, _ -> false }
                        }
                    },
                    update = { view ->
                        if (view.player != player) {
                            view.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Transparent Gesture Area for reliable Single Tap and Double Tap
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(page, videos.size) {
                            detectHorizontalDragGestures(
                                onDragStart = { horizontalDragX = 0f },
                                onDragEnd = {
                                    if (horizontalDragX < -120f && page < videos.size - 1) {
                                        // Swipe Left -> Next Video
                                        scope.launch { pagerState.animateScrollToPage(page + 1) }
                                    } else if (horizontalDragX > 120f && page > 0) {
                                        // Swipe Right -> Prev Video
                                        scope.launch { pagerState.animateScrollToPage(page - 1) }
                                    }
                                    horizontalDragX = 0f
                                },
                                onDragCancel = { horizontalDragX = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    horizontalDragX += dragAmount
                                }
                            )
                        }
                        .pointerInput(page) {
                            detectTapGestures(
                                onTap = {
                                    // Single tap: toggle OSD visibility
                                    isOsdVisible = !isOsdVisible
                                },
                                onDoubleTap = {
                                    // Double tap: toggle Play / Pause with visual indicator and reveal OSD
                                    togglePlayPause()
                                }
                            )
                        }
                )
            }
        }

        // Center animated Play/Pause pulse overlay on double-tap
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PlayPausePulseOverlay(
                isPlaying = isPlayingState,
                triggerToken = pulseToken
            )
        }

        // OSD Overlay (Top metadata + Mode Cycler + Circular Playlist Badges)
        OsdOverlay(
            isVisible = isOsdVisible,
            currentVideo = activeVideo,
            currentIndex = currentPage,
            totalVideos = videos.size,
            playMode = playMode,
            playbackSpeed = playbackSpeed,
            activePlaylistIds = activePlaylistIdsForCurrentVideo,
            onCyclePlayMode = onCyclePlayMode,
            onSpeedChanged = onSpeedChanged,
            onTogglePlaylist = { playlistId ->
                if (activeVideo != null) {
                    onTogglePlaylist(activeVideo.id, playlistId)
                }
            },
            onBack = {
                if (activeVideo != null && currentPage in videos.indices) {
                    val player = playerPool.getOrCreatePlayer(currentPage, activeVideo)
                    onSavePosition(activeVideo.id, player.currentPosition)
                }
                onBack()
            }
        )

        // Bottom Seeking Bar: thin draggable line at the very bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            VideoProgressBar(
                currentPositionMs = currentPositionMs,
                durationMs = currentDurationMs,
                onSeek = { targetMs ->
                    if (currentPage in videos.indices) {
                        val player = playerPool.getOrCreatePlayer(currentPage, videos[currentPage])
                        player.seekTo(targetMs)
                        currentPositionMs = targetMs
                    }
                }
            )
        }
    }
}
