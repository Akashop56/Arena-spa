package com.ronin.ai.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ronin.ai.core.design.theme.RoninAmber
import com.ronin.ai.core.design.theme.RoninBlack
import com.ronin.ai.core.design.theme.RoninBorder
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninSurfaceHigh
import com.ronin.ai.core.design.theme.RoninTextSecondary

/**
 * Click handler without the Material ripple. Used for small inline affordances
 * (copy chips, stage pills) where a full ripple looks heavy.
 */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/**
 * App-wide HUD background.
 *
 * This used to decode a 1376x768 PNG (~4 MB of ARGB_8888 in RAM) on every
 * screen. It is now drawn entirely by the GPU with two radial gradients, which
 * costs no bitmap memory and no decode time — a meaningful win on the low-end
 * devices RONIN targets — while keeping the same deep-space look.
 */
@Composable
fun RoninBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoninBlack)
            .drawBehind {
                // Cyan glow from the top-left, amber counter-glow bottom-right.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(RoninCyan.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.08f),
                        radius = size.maxDimension * 0.75f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(RoninAmber.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.85f),
                        radius = size.maxDimension * 0.6f
                    )
                )
                // Vignette so content stays legible near the bottom bar.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, RoninBlack.copy(alpha = 0.85f)),
                        startY = size.height * 0.55f,
                        endY = size.height
                    )
                )
            }
    ) {
        content()
    }
}

/** Glassy card with a neon-tinted border. */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(RoninSurfaceHigh.copy(alpha = 0.9f), RoninSurfaceHigh.copy(alpha = 0.55f))
                )
            )
            .border(
                width = 1.dp,
                color = if (glow) RoninCyan.copy(alpha = 0.28f) else RoninBorder,
                shape = shape
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = RoninTextSecondary
        )
        trailing?.let { it() }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RoninSurfaceHigh)
                .border(1.dp, RoninBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RoninCyan, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = RoninTextSecondary,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = RoninTextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Small stat tile used on the dashboard. */
@Composable
fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = RoninCyan
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(RoninSurfaceHigh.copy(alpha = 0.7f))
            .border(1.dp, RoninBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = RoninTextSecondary)
    }
}
