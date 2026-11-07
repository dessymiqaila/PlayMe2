package com.example.playme.ui.player

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.playme.data.db.VideoEntity
import com.example.playme.data.model.PlayOrderMode
import com.example.playme.data.scanner.FolderScanner
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
    videoPlaylistMap: Map<Long, Set<Int>> = emptyMap(),
    onCyclePlayMode: () -> Unit,
    onSpeedChanged: (Float) -> Unit = {},
    onTogglePlaylist: (videoId: Long, playlistId: Int) -> Unit,
    onDeleteVideo: (videoId: Long) -> Unit = {},
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
    val safeInitialIndex = initialIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
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

    // Lifecycle Observer: Stop/Pause video when app goes to background
    val lifecycleOwner = LocalLifecycleOwner.current
    var isPlayingState by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                playerPool.pauseAll()
                isPlayingState = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Default: hidden OSD and playlist stack
    var isOsdVisible by remember { mutableStateOf(false) }
    var pulseToken by remember { mutableLongStateOf(0L) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var currentDurationMs by remember { mutableLongStateOf(0L) }

    val currentPage = pagerState.currentPage.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
    val activeVideo = videos.getOrNull(currentPage)

    // Reactive active playlist IDs for the current video (supports multiple playlists!)
    val activePlaylistIds = activeVideo?.let { videoPlaylistMap[it.id] } ?: emptySet()

    // Notify video change immediately on activeVideo change
    LaunchedEffect(activeVideo?.id, currentPage) {
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
        if (currentPage in videos.indices) {
            playerPool.updateActiveWindow(currentPage, videos)
            val activePlayer = playerPool.getOrCreatePlayer(currentPage, videos[currentPage])
            activePlayer.repeatMode = Player.REPEAT_MODE_ONE // TikTok style continuous loop
            activePlayer.play()
            isPlayingState = true
        }
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
            val video = videos.getOrNull(page)
            if (video != null) {
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

        // OSD Overlay (Top metadata + Speed Controller + Mode Cycler + Circular Playlist Badges)
        OsdOverlay(
            isVisible = isOsdVisible,
            currentVideo = activeVideo,
            currentIndex = currentPage,
            totalVideos = videos.size,
            playMode = playMode,
            playbackSpeed = playbackSpeed,
            activePlaylistIds = activePlaylistIds,
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

        // Bottom Controls Container: Pause File Info & Action Bar + Seeking Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Pause Action Bar: Shows File Info, Share Button, and Delete Button when video is PAUSED
            AnimatedVisibility(
                visible = !isPlayingState && activeVideo != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                if (activeVideo != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("pause_info_action_bar"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xDC151922),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333D52))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // File Info: title, size, duration, date
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = activeVideo.fileName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = FolderScanner.formatFileSize(activeVideo.fileSizeBytes),
                                        color = Color(0xFFFFAB40),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = " • ${FolderScanner.formatDuration(activeVideo.durationMs)}",
                                        color = Color(0xFFA1A8B8),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = " • ${FolderScanner.formatTimestamp(activeVideo.dateModified)}",
                                        color = Color(0xFF6C7689),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Actions: Share & Delete
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Share Button
                                IconButton(
                                    onClick = {
                                        try {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "video/*"
                                                val uri = Uri.parse(activeVideo.uriString)
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_TEXT, "Watch: ${activeVideo.fileName}")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                        } catch (e: Exception) {
                                            val textIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Video: ${activeVideo.fileName}")
                                            }
                                            context.startActivity(Intent.createChooser(textIntent, "Share Video"))
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF222938))
                                        .testTag("pause_share_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share File",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete Button
                                IconButton(
                                    onClick = { showDeleteConfirmDialog = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FF1744))
                                        .testTag("pause_delete_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete File",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Seeking Bar: thin draggable line at the very bottom
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

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog && activeVideo != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(text = "Delete Video", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Are you sure you want to remove \"${activeVideo.fileName}\" from your library?",
                        color = Color(0xFFA1A8B8)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteVideo(activeVideo.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1C202C),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
