package com.example.playme.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playme.data.scanner.FolderScanner
import kotlin.math.roundToInt

@Composable
fun VideoProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val effectiveFraction = if (isDragging) {
        scrubFraction
    } else {
        if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val barHeight by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 3.dp,
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp) // Touch target area while keeping the visible bar thin
            .testTag("video_progress_bar"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Scrub bubble preview when dragging
        if (isDragging && durationMs > 0) {
            val scrubTimeMs = (scrubFraction * durationMs).toLong()
            val text = "${FolderScanner.formatDuration(scrubTimeMs)} / ${FolderScanner.formatDuration(durationMs)}"
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        // Offset bubble horizontally based on thumb position
                        IntOffset(0, -with(density) { 16.dp.roundToPx() })
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && durationMs > 0) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            if (size.width > 0) {
                                scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            if (durationMs > 0) {
                                onSeek((scrubFraction * durationMs).toLong())
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (size.width > 0) {
                                scrubFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height

            // Background track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )

            // Progress bar
            val progressWidth = w * effectiveFraction
            drawRoundRect(
                color = Color(0xFFFF6D00),
                size = Size(progressWidth, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )

            // Thumb on drag
            if (isDragging) {
                drawCircle(
                    color = Color.White,
                    radius = (h * 1.4f),
                    center = Offset(progressWidth.coerceIn(0f, w), h / 2f)
                )
            }
        }
    }
}
