package com.example.playme.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playme.data.db.VideoEntity
import com.example.playme.data.model.PlayOrderMode
import com.example.playme.data.scanner.FolderScanner
import kotlin.math.roundToInt

@Composable
fun OsdOverlay(
    isVisible: Boolean,
    currentVideo: VideoEntity?,
    currentIndex: Int,
    totalVideos: Int,
    playMode: PlayOrderMode,
    playbackSpeed: Float,
    activePlaylistIds: Set<Int>,
    badgesAlwaysVisible: Boolean,
    badgeOffsetY: Float,
    onToggleBadgesAlwaysVisible: () -> Unit,
    onSaveBadgeOffset: (Float) -> Unit,
    onCyclePlayMode: () -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onTogglePlaylist: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedSelector by remember { mutableStateOf(false) }
    var currentBadgeOffsetY by remember(badgeOffsetY) { mutableFloatStateOf(badgeOffsetY) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Gradient vignettes
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        // Top Bar: Back button, Video info, Speed Controller, Mode Toggle (icon only), and Badge Visibility Pin
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back & Metadata Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("osd_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.padding(start = 10.dp)
                    ) {
                        Text(
                            text = currentVideo?.fileName ?: "PlayMe",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentIndex + 1} / $totalVideos",
                                color = Color(0xFFFFAB40),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (currentVideo != null) {
                                Text(
                                    text = " • ${FolderScanner.formatFileSize(currentVideo.fileSizeBytes)}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = " • ${FolderScanner.formatDuration(currentVideo.durationMs)}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Top Right Action Buttons: Speed Pill, Order Mode (Icon Only), and Badges Pin Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Speed Controller Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (playbackSpeed != 1.0f) Color(0xFFFF6D00) else Color.Black.copy(alpha = 0.55f)
                            )
                            .clickable { showSpeedSelector = !showSpeedSelector }
                            .padding(horizontal = 9.dp, vertical = 7.dp)
                            .testTag("osd_speed_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = "Playback Speed Controller",
                                tint = if (playbackSpeed != 1.0f) Color.White else Color(0xFFFFAB40),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Playback Order Mode Cycle Toggle - ICON ONLY (no text per user request)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onCyclePlayMode() }
                            .testTag("osd_mode_cycle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (playMode) {
                            PlayOrderMode.SHUFFLE -> Icons.Filled.Shuffle
                            PlayOrderMode.NAME_AZ -> Icons.Filled.SortByAlpha
                            PlayOrderMode.DATE_MODIFIED -> Icons.Filled.CalendarToday
                            PlayOrderMode.DURATION -> Icons.Filled.AvTimer
                            PlayOrderMode.FILE_SIZE -> Icons.Filled.Storage
                            PlayOrderMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = playMode.label,
                            tint = Color(0xFFFF6D00),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Playlist Badges Visibility Pin Toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (badgesAlwaysVisible) Color(0xFFFF6D00) else Color.Black.copy(alpha = 0.55f)
                            )
                            .clickable { onToggleBadgesAlwaysVisible() }
                            .testTag("osd_badge_visibility_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (badgesAlwaysVisible) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (badgesAlwaysVisible) "Playlist Badges: Always Visible" else "Playlist Badges: On Touch Only",
                            tint = if (badgesAlwaysVisible) Color.White else Color(0xFFA1A8B8),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // Expandable Speed Selector Menu
        AnimatedVisibility(
            visible = isVisible && showSpeedSelector,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 56.dp, end = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF0181C26),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF353F57)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYBACK SPEED",
                        color = Color(0xFFA1A8B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        speeds.forEach { spd ->
                            val isSelected = playbackSpeed == spd
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFF6D00) else Color(0xFF242A3A)
                                    )
                                    .clickable {
                                        onSpeedChanged(spd)
                                        showSpeedSelector = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("speed_option_${spd}x"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${spd}x",
                                    color = if (isSelected) Color.White else Color(0xFFCCD2E0),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right-Side Playlist Icons (1..6)
        // Can be always visible or visible on touch according to badgesAlwaysVisible setting
        val shouldShowBadges = badgesAlwaysVisible || isVisible

        AnimatedVisibility(
            visible = shouldShowBadges,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(0, currentBadgeOffsetY.roundToInt()) }
                .padding(end = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { onSaveBadgeOffset(currentBadgeOffsetY) },
                            onDragCancel = { onSaveBadgeOffset(currentBadgeOffsetY) }
                        ) { change, dragAmount ->
                            change.consume()
                            currentBadgeOffsetY = (currentBadgeOffsetY + dragAmount).coerceIn(-300f, 220f)
                        }
                    }
                    .testTag("right_playlist_container"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sleek Drag Handle at top of badge column
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp, bottom = 2.dp)
                        .size(width = 24.dp, height = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Drag to reposition playlist badges",
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 6 Circular Playlist Badges (1..6)
                (1..6).forEach { playlistNum ->
                    val isInPlaylist = activePlaylistIds.contains(playlistNum)
                    PlaylistCircleBadge(
                        playlistNumber = playlistNum,
                        isActive = isInPlaylist,
                        onToggle = { onTogglePlaylist(playlistNum) },
                        size = 40.dp
                    )
                }
            }
        }
    }
}
