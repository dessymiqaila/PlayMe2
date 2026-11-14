package com.example.playme.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
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
    progressBarScale: Int,
    abStartMs: Long?,
    abEndMs: Long?,
    activePlaylistIds: Set<Int>,
    badgesAlwaysVisible: Boolean,
    badgeOffsetY: Float,
    onToggleBadgesAlwaysVisible: () -> Unit,
    onSaveBadgeOffset: (Float) -> Unit,
    onCyclePlayMode: () -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onProgressBarScaleChanged: (Int) -> Unit,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClearAbLoop: () -> Unit,
    onTogglePlaylist: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedSelector by remember { mutableStateOf(false) }
    var showAbMenu by remember { mutableStateOf(false) }
    var currentBadgeOffsetY by remember(badgeOffsetY) { mutableFloatStateOf(badgeOffsetY) }

    val isAbActive = abStartMs != null && abEndMs != null
    val isOnlyASet = abStartMs != null && abEndMs == null

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
                                Color.Black.copy(alpha = 0.78f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        // Persistent compact A-B Active badge when OSD is hidden so user remembers A-B loop is running
        if (!isVisible && (isAbActive || isOnlyASet)) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC0D1B2A))
                    .clickable { onClearAbLoop() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("ab_active_chip"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = "A-B Loop Active",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(14.dp)
                )
                val label = if (isAbActive) {
                    "${FolderScanner.formatDuration(abStartMs!!)} - ${FolderScanner.formatDuration(abEndMs!!)}"
                } else {
                    "A: ${FolderScanner.formatDuration(abStartMs!!)}"
                }
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear A-B Loop",
                    tint = Color(0xFFA1A8B8),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Top Bar: Back button, Video info, A-B Loop, Speed/Bar Controller, Mode Toggle (icon only), and Badge Pin
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("osd_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.padding(start = 8.dp, end = 6.dp)
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
                                text = "${currentIndex + 1}/$totalVideos",
                                color = Color(0xFFFFAB40),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (currentVideo != null) {
                                Text(
                                    text = " • ${FolderScanner.formatFileSize(currentVideo.fileSizeBytes)}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Top Right Action Buttons: A-B Loop, Speed/Bar Pill, Order Mode (Icon Only), Pin Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // A-B Loop Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                when {
                                    isAbActive -> Color(0xFF0097A7)
                                    isOnlyASet -> Color(0xFFFF8F00)
                                    showAbMenu -> Color(0xFF2B3548)
                                    else -> Color.Black.copy(alpha = 0.55f)
                                }
                            )
                            .clickable {
                                showAbMenu = !showAbMenu
                                if (showAbMenu) showSpeedSelector = false
                            }
                            .padding(horizontal = 9.dp, vertical = 7.dp)
                            .testTag("osd_ab_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A-B",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Speed & Bar Size Controller Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (playbackSpeed != 1.0f || showSpeedSelector) Color(0xFFFF6D00) else Color.Black.copy(alpha = 0.55f)
                            )
                            .clickable {
                                showSpeedSelector = !showSpeedSelector
                                if (showSpeedSelector) showAbMenu = false
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                            .testTag("osd_speed_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = "Speed and Bar Size",
                                tint = if (playbackSpeed != 1.0f || showSpeedSelector) Color.White else Color(0xFFFFAB40),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Playback Order Mode Cycle Toggle - ICON ONLY
                    Box(
                        modifier = Modifier
                            .size(34.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Playlist Badges Visibility Pin Toggle
                    Box(
                        modifier = Modifier
                            .size(34.dp)
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
                            contentDescription = if (badgesAlwaysVisible) "Badges Always Visible" else "Badges On Touch",
                            tint = if (badgesAlwaysVisible) Color.White else Color(0xFFA1A8B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Expandable A-B Repeat Loop Menu
        AnimatedVisibility(
            visible = isVisible && showAbMenu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 56.dp, end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF2151923),
                border = BorderStroke(1.dp, Color(0xFF00B8D4)),
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("ab_loop_menu")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "A-B REPEAT LOOP",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "A: ${abStartMs?.let { FolderScanner.formatDuration(it) } ?: "--:--"}",
                            color = if (abStartMs != null) Color(0xFF00E5FF) else Color(0xFFA1A8B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "→",
                            color = Color(0xFF6C7689),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "B: ${abEndMs?.let { FolderScanner.formatDuration(it) } ?: "--:--"}",
                            color = if (abEndMs != null) Color(0xFF00E676) else Color(0xFFA1A8B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (abStartMs != null) Color(0xFF00838F) else Color(0xFF242A3A))
                                .clickable { onSetPointA() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("ab_set_a_button")
                        ) {
                            Text(
                                text = "Set A",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (abEndMs != null) Color(0xFF2E7D32) else Color(0xFF242A3A))
                                .clickable { onSetPointB() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("ab_set_b_button")
                        ) {
                            Text(
                                text = "Set B",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33FF1744))
                                .clickable {
                                    onClearAbLoop()
                                    showAbMenu = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("ab_clear_button")
                        ) {
                            Text(
                                text = "Clear",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Expandable Speed & Progress Bar Size Selector Menu
        AnimatedVisibility(
            visible = isVisible && showSpeedSelector,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 56.dp, end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF0181C26),
                border = BorderStroke(1.dp, Color(0xFF353F57)),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SEEK BAR SIZE (ON TOUCH)",
                        color = Color(0xFFA1A8B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3, 4).forEach { scale ->
                            val isSelected = progressBarScale == scale
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFF6D00) else Color(0xFF242A3A)
                                    )
                                    .clickable {
                                        onProgressBarScaleChanged(scale)
                                    }
                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                                    .testTag("osd_bar_scale_${scale}x"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${scale}x",
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

        // Right-Side Playlist Icons (1..6) - Transparent outer container (no box!)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Drag handle shown subtly only when OSD is tapped so floating circles have zero clutter
                if (isVisible) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Drag to reposition playlist badges",
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 6 Floating Circular Playlist Badges (1..6)
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
