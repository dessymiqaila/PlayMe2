package com.example.playme.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun CrossedCarrotIcon(
    playlistNumber: Int,
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isActive) {
        scaleAnim.animateTo(
            targetValue = 1.25f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
        )
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    val activeOrange = Color(0xFFFF6D00)
    val activeAmber = Color(0xFFFFAB40)
    val activeGreen = Color(0xFF00E676)
    val inactiveOutline = Color.White.copy(alpha = 0.55f)
    val inactiveBackground = Color.Black.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .size(54.dp)
            .scale(scaleAnim.value)
            .clip(CircleShape)
            .background(if (isActive) Color(0x33FF6D00) else inactiveBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch {
                    scaleAnim.snapTo(0.85f)
                    scaleAnim.animateTo(
                        1.0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }
                onToggle()
            }
            .testTag("playlist_carrot_toggle_$playlistNumber"),
        contentAlignment = Alignment.Center
    ) {
        // Draw the two crossed carrots (vector style like crossed swords)
        Canvas(modifier = Modifier.size(46.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val carrotBrush = if (isActive) {
                Brush.linearGradient(
                    colors = listOf(activeAmber, activeOrange),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(inactiveOutline, inactiveOutline.copy(alpha = 0.3f)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            }

            // Draw Carrot 1: pointing from top-left to bottom-right (tapering down-right)
            rotate(degrees = 42f, pivot = Offset(cx, cy)) {
                drawCarrot(
                    cx = cx,
                    cy = cy,
                    w = w,
                    h = h,
                    brush = carrotBrush,
                    leafColor = if (isActive) activeGreen else inactiveOutline.copy(alpha = 0.5f),
                    isActive = isActive
                )
            }

            // Draw Carrot 2: pointing from top-right to bottom-left (tapering down-left)
            rotate(degrees = -42f, pivot = Offset(cx, cy)) {
                drawCarrot(
                    cx = cx,
                    cy = cy,
                    w = w,
                    h = h,
                    brush = carrotBrush,
                    leafColor = if (isActive) activeGreen else inactiveOutline.copy(alpha = 0.5f),
                    isActive = isActive
                )
            }

            // Central circle badge to house the number
            val centerRadius = 11.dp.toPx()
            if (isActive) {
                drawCircle(
                    color = Color(0xFF1E140A),
                    radius = centerRadius,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = activeOrange,
                    radius = centerRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            } else {
                drawCircle(
                    color = Color(0xCC11141B),
                    radius = centerRadius,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = inactiveOutline,
                    radius = centerRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f.dp.toPx())
                )
            }
        }

        // Centered Playlist Number
        Text(
            text = playlistNumber.toString(),
            color = if (isActive) Color(0xFFFFD180) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCarrot(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    brush: Brush,
    leafColor: Color,
    isActive: Boolean
) {
    // Carrot body: elongated cone from top (leaves) tapering down to point (root)
    // Centered horizontally around cx
    val topY = cy - (h * 0.42f)
    val bottomY = cy + (h * 0.42f)
    val rootWidth = w * 0.16f

    val carrotPath = Path().apply {
        // Top shoulder
        moveTo(cx - rootWidth / 2f, topY + (h * 0.12f))
        // Curve to right shoulder
        quadraticTo(cx, topY + (h * 0.08f), cx + rootWidth / 2f, topY + (h * 0.12f))
        // Tapering line down to bottom root tip
        lineTo(cx, bottomY)
        // Close back to left shoulder
        close()
    }

    if (isActive) {
        drawPath(path = carrotPath, brush = brush)
        // Horizontal carrot creases
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(cx - rootWidth * 0.3f, cy - (h * 0.18f)),
            end = Offset(cx + rootWidth * 0.2f, cy - (h * 0.18f)),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(cx - rootWidth * 0.2f, cy + (h * 0.18f)),
            end = Offset(cx + rootWidth * 0.25f, cy + (h * 0.18f)),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
    } else {
        drawPath(
            path = carrotPath,
            brush = brush,
            style = Stroke(width = 1.8f.dp.toPx())
        )
    }

    // Top Leaf plumes (3 sprigs)
    val leafBaseY = topY + (h * 0.10f)
    // Center sprig
    drawLine(
        color = leafColor,
        start = Offset(cx, leafBaseY),
        end = Offset(cx, topY - (h * 0.03f)),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Left sprig
    drawLine(
        color = leafColor,
        start = Offset(cx - 2f, leafBaseY),
        end = Offset(cx - (w * 0.08f), topY + (h * 0.01f)),
        strokeWidth = 1.8f.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Right sprig
    drawLine(
        color = leafColor,
        start = Offset(cx + 2f, leafBaseY),
        end = Offset(cx + (w * 0.08f), topY + (h * 0.01f)),
        strokeWidth = 1.8f.dp.toPx(),
        cap = StrokeCap.Round
    )
}
