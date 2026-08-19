package com.ronin.ai.core.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronin.ai.core.design.theme.RoninBorder
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSurfaceHigh
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninViolet
import com.ronin.ai.core.design.theme.RoninWarning

/** Small status pill with a dot. */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

fun statusColor(ok: Boolean): Color = if (ok) RoninSuccess else RoninError

/** Three bouncing dots while the brain is working. */
@Composable
fun LoadingDots(modifier: Modifier = Modifier, color: Color = RoninCyan) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val offset by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 120, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(0.3f + 0.7f * offset)
                    .background(color, CircleShape)
            )
        }
    }
}

/** Pulsing glow orb for the dashboard header. */
@Composable
fun GlowOrb(modifier: Modifier = Modifier, color: Color = RoninCyan) {
    val transition = rememberInfiniteTransition(label = "orb")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )
    Box(
        modifier = modifier.size((48 * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size((44 * scale).dp)
                .background(color.copy(alpha = 0.25f), CircleShape)
        )
        Box(
            Modifier
                .size((30 * scale).dp)
                .background(color.copy(alpha = 0.45f), CircleShape)
        )
        Box(
            Modifier
                .size(18.dp)
                .background(color, CircleShape)
        )
    }
}

/** Animated waveform bars for the voice screen. */
@Composable
fun WaveformBars(
    active: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 7,
    color: Color = RoninCyan
) {
    val transition = rememberInfiniteTransition(label = "wave")
    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(450 + index * 70, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            val h = if (active) 10 + 26 * phase else 8f
            Box(
                Modifier
                    .width(6.dp)
                    .height(h.dp)
                    .background(
                        if (index % 2 == 0) color else RoninViolet,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/** Reasoning pipeline stage indicator (public stages only). */
@Composable
fun PipelineStageRow(stageLabel: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(RoninSurfaceHigh.copy(alpha = 0.8f), RoundedCornerShape(50))
            .border(1.dp, RoninBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoadingDots(color = RoninCyan)
        Text(
            stageLabel ?: "Thinking…",
            style = MaterialTheme.typography.labelMedium,
            color = RoninTextSecondary
        )
    }
}

/** Inline error banner. */
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(RoninError.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, RoninError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(RoninError, CircleShape))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = RoninError,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}
