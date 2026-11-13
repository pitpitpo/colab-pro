package com.pox.colab.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pox.colab.domain.model.VmStatus

/**
 * Expressive M3 shimmer brush for skeleton placeholders.
 */
@Composable
fun rememberExpressiveShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "m3_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val colors = listOf(
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerLow
    )

    return Brush.linearGradient(
        colors = colors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 200f)
    )
}

@Composable
fun ButtonPlaceholder(
    width: Dp = 104.dp,
    modifier: Modifier = Modifier
) {
    val brush = rememberExpressiveShimmerBrush()
    Box(
        modifier = modifier
            .width(width)
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
    )
}

@Composable
fun ListItemPlaceholder(modifier: Modifier = Modifier) {
    val brush = rememberExpressiveShimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
            }
        }
        ButtonPlaceholder(width = 72.dp)
    }
}

@Composable
fun GridItemPlaceholder(modifier: Modifier = Modifier) {
    val brush = rememberExpressiveShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(brush)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ButtonPlaceholder(width = 112.dp)
            ButtonPlaceholder(width = 88.dp)
        }
    }
}

/**
 * Pulsing live status dot (`StatusIndicatorDot`) with infinite alpha tween.
 */
@Composable
fun StatusIndicatorDot(
    status: VmStatus,
    modifier: Modifier = Modifier
) {
    val isLive = status == VmStatus.Running || status == VmStatus.Booting || status == VmStatus.Reconnecting
    val transition = rememberInfiniteTransition(label = "status_dot_alpha")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isLive) 0.32f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_tween"
    )

    val dotColor = when (status) {
        VmStatus.Running -> Color(0xFF34D399)
        VmStatus.Reconnecting -> Color(0xFFFBBF24)
        VmStatus.Booting -> Color(0xFF38BDF8)
        VmStatus.Stopped -> Color(0xFF71717A)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = status.name,
            color = dotColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
