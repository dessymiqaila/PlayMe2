package com.example.playme.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playme.data.db.VideoEntity
import com.example.playme.data.model.PlayOrderMode
import com.example.playme.data.scanner.FolderScanner

@Composable
fun OsdOverlay(
    isVisible: Boolean,
    currentVideo: VideoEntity?,
    currentIndex: Int,
    totalVideos: Int,
    playMode: PlayOrderMode,
    playbackSpeed: Float,
    activePlaylistIds: Set<Int>,
    onCyclePlayMode: () -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onTogglePlaylist: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedSelector by remember { mutableStateOf(false) }

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

        // Top Bar: Back button, Video info, Speed Controller, and Order Mode cycle button
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            .size(44.dp)
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
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            text = currentVideo?.fileName ?: "PlayMe",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentIndex + 1} / $totalVideos",
                                color = Color(0xFFFFAB40),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (currentVideo != null) {
                                Text(
                                    text = " • ${FolderScanner.formatFileSize(currentVideo.fileSizeBytes)}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = " • ${FolderScanner.formatDuration(currentVideo.durationMs)}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Top Right: Speed Controller Pill & Order Mode Cycle Toggle
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
                            .padding(horizontal = 10.dp, vertical = 8.dp)
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
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Playback Order Mode Cycle Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onCyclePlayMode() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("osd_mode_cycle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = playMode.shortLabel,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                .padding(top = 58.dp, end = 16.dp)
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
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 54.dp)
        ) {
            Column(
                modifier = Modifier.testTag("right_playlist_container"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..6).forEach { playlistNum ->
                    val isInPlaylist = activePlaylistIds.contains(playlistNum)
                    PlaylistCircleBadge(
                        playlistNumber = playlistNum,
                        isActive = isInPlaylist,
                        onToggle = { onTogglePlaylist(playlistNum) },
                        size = 42.dp
                    )
                }
            }
        }
    }
}
