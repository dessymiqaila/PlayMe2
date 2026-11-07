package com.example.playme.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlayPausePulseOverlay(
    isPlaying: Boolean,
    triggerToken: Long,
    modifier: Modifier = Modifier
) {
    if (triggerToken == 0L) return

    val scale = remember(triggerToken) { Animatable(0.7f) }
    val alpha = remember(triggerToken) { Animatable(1.0f) }

    LaunchedEffect(triggerToken) {
        scale.animateTo(
            targetValue = 1.15f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    if (alpha.value > 0.01f) {
        Box(
            modifier = modifier
                .size(80.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f * alpha.value)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (isPlaying) "Playing" else "Paused",
                tint = Color.White.copy(alpha = alpha.value),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
