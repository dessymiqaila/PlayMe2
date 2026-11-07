package com.example.playme.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PlaylistCircleBadge(
    playlistNumber: Int,
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val scaleAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isActive) {
        scaleAnim.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
        )
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    val activeColor = Color(0xFFFF6D00)
    val activeBg = Color(0x4DFF6D00)
    val inactiveBorder = Color.White.copy(alpha = 0.55f)
    val inactiveBg = Color.Black.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .size(size)
            .scale(scaleAnim.value)
            .clip(CircleShape)
            .background(if (isActive) activeBg else inactiveBg)
            .border(
                width = if (isActive) 2.5.dp else 1.5.dp,
                color = if (isActive) activeColor else inactiveBorder,
                shape = CircleShape
            )
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
            .testTag("playlist_badge_toggle_$playlistNumber"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = playlistNumber.toString(),
            color = if (isActive) Color(0xFFFFD180) else Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Black
        )
    }
}
