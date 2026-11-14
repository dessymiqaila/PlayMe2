package com.example.playme.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playme.data.scanner.FolderScanner

@Composable
fun VideoProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    isExpanded: Boolean = false,
    sizeMultiplier: Int = 3,
    abStartMs: Long? = null,
    abEndMs: Long? = null,
    onSeek: (Long) -> Unit,
    onDraggingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val effectiveFraction = if (isDragging) {
        scrubFraction
    } else {
        if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val clampedMultiplier = sizeMultiplier.coerceIn(1, 4)
    val baseHeightDp = 3f
    val targetBarHeight = when {
        isDragging -> (baseHeightDp * clampedMultiplier + 3f).dp
        isExpanded -> (baseHeightDp * clampedMultiplier).dp
        else -> baseHeightDp.dp
    }

    val barHeight by animateDpAsState(
        targetValue = targetBarHeight,
        animationSpec = tween(durationMillis = 180),
        label = "progress_bar_height"
    )

    val displayedTimeMs = if (isDragging && durationMs > 0) {
        (scrubFraction * durationMs).toLong()
    } else {
        currentPositionMs
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_progress_bar")
    ) {
        // Large scrub preview tooltip when actively dragging
        AnimatedVisibility(
            visible = isDragging && durationMs > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xE6151922))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${FolderScanner.formatDuration(displayedTimeMs)} / ${FolderScanner.formatDuration(durationMs)}",
                    color = Color(0xFFFFAB40),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Timestamp row shown when progress bar is expanded (single tap or paused)
        AnimatedVisibility(
            visible = (isExpanded || isDragging) && durationMs > 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FolderScanner.formatDuration(displayedTimeMs),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (abStartMs != null || abEndMs != null) {
                    val aTxt = abStartMs?.let { FolderScanner.formatDuration(it) } ?: "--:--"
                    val bTxt = abEndMs?.let { FolderScanner.formatDuration(it) } ?: "--:--"
                    Text(
                        text = "A-B Loop: $aTxt → $bTxt",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = FolderScanner.formatDuration(durationMs),
                    color = Color(0xFFA1A8B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Interactive Track Container (large 44dp touch area when expanded so thumb/track is easy to grab)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isExpanded || isDragging) 44.dp else 28.dp)
                .padding(horizontal = if (isExpanded || isDragging) 12.dp else 0.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && durationMs > 0) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            onDraggingChanged(true)
                            if (size.width > 0) {
                                scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                if (durationMs > 0) {
                                    onSeek((scrubFraction * durationMs).toLong())
                                }
                            }
                        },
                        onDragEnd = {
                            if (durationMs > 0) {
                                onSeek((scrubFraction * durationMs).toLong())
                            }
                            isDragging = false
                            onDraggingChanged(false)
                        },
                        onDragCancel = {
                            isDragging = false
                            onDraggingChanged(false)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (size.width > 0) {
                                scrubFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                if (durationMs > 0) {
                                    onSeek((scrubFraction * durationMs).toLong())
                                }
                            }
                        }
                    )
                },
            contentAlignment = if (isExpanded || isDragging) Alignment.Center else Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
            ) {
                val w = size.width
                val h = size.height

                // Background track
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isExpanded || isDragging) 0.35f else 0.25f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )

                // A-B Loop Highlight Span if both or one point is set
                if (durationMs > 0 && abStartMs != null) {
                    val aFrac = (abStartMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    if (abEndMs != null) {
                        val bFrac = (abEndMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        val startFrac = minOf(aFrac, bFrac)
                        val endFrac = maxOf(aFrac, bFrac)
                        val spanW = ((endFrac - startFrac) * w).coerceAtLeast(4f)
                        drawRoundRect(
                            color = Color(0x8800E5FF),
                            topLeft = Offset(startFrac * w, 0f),
                            size = Size(spanW, h),
                            cornerRadius = CornerRadius(h / 2f, h / 2f)
                        )
                    }
                }

                // Active Playback Progress Bar
                val progressWidth = (w * effectiveFraction).coerceIn(0f, w)
                drawRoundRect(
                    color = Color(0xFFFF6D00),
                    size = Size(progressWidth, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )

                // Draw A and B marker pips on the bar
                if (durationMs > 0) {
                    if (abStartMs != null) {
                        val ax = (abStartMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = (h * 0.65f).coerceAtLeast(5f),
                            center = Offset(ax, h / 2f)
                        )
                    }
                    if (abEndMs != null) {
                        val bx = (abEndMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = (h * 0.65f).coerceAtLeast(5f),
                            center = Offset(bx, h / 2f)
                        )
                    }
                }

                // Draggable Thumb Knob shown when expanded (tap) or actively dragging
                if (isExpanded || isDragging) {
                    val knobRadius = (h * 1.15f).coerceIn(9.dp.toPx(), 14.dp.toPx())
                    // Outer accent halo
                    drawCircle(
                        color = Color(0x66FF6D00),
                        radius = knobRadius * 1.45f,
                        center = Offset(progressWidth, h / 2f)
                    )
                    // Inner bright white thumb
                    drawCircle(
                        color = Color.White,
                        radius = knobRadius,
                        center = Offset(progressWidth, h / 2f)
                    )
                }
            }
        }
    }
}
