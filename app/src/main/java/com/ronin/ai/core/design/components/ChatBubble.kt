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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ronin.ai.core.common.TimeFormat
import com.ronin.ai.core.design.theme.RoninBlack
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninSurfaceHigh
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.domain.model.ChatMessage
import com.ronin.ai.core.domain.model.ChatRole
import kotlinx.coroutines.delay

/**
 * Assistant bubble for a reply that is still streaming. Shows the partial
 * Markdown plus a blinking caret; no copy action until the reply is complete.
 */
@Composable
fun StreamingBubble(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    val transition = rememberInfiniteTransition(label = "caret")
    val caretAlpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caretAlpha"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(0.92f)
                .clip(shape)
                .background(RoninSurfaceHigh)
                .border(1.dp, RoninCyan.copy(alpha = 0.32f), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            MarkdownText(text = text)
            Text(
                text = "▋",
                style = MaterialTheme.typography.labelMedium,
                color = RoninCyan.copy(alpha = caretAlpha)
            )
        }
    }
}

/**
 * One conversation turn. Assistant replies render Markdown and expose a copy
 * action; user turns stay plain text on a cyan gradient.
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    showCopy: Boolean = true
) {
    val isUser = message.role == ChatRole.USER
    val shape = if (isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(0.92f)
                .clip(shape)
                .let {
                    if (isUser) {
                        it.background(
                            Brush.horizontalGradient(listOf(RoninCyan, RoninCyan.copy(alpha = 0.75f)))
                        )
                    } else {
                        it
                            .background(RoninSurfaceHigh)
                            .border(1.dp, RoninCyan.copy(alpha = 0.22f), shape)
                    }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoninBlack
                )
            } else {
                MarkdownText(text = message.content)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        if (message.toolUsed != null) append("🔧 ${message.toolUsed} · ")
                        append(TimeFormat.clock(message.timestamp))
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) RoninBlack.copy(alpha = 0.6f) else RoninTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                if (!isUser && showCopy && message.content.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(RoninCyan.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .then(
                                Modifier.clickableNoRipple {
                                    clipboard.setText(AnnotatedString(message.content))
                                    copied = true
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = "Copy response",
                            tint = if (copied) RoninSuccess else RoninCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            if (copied) "Copied" else "Copy",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (copied) RoninSuccess else RoninCyan
                        )
                    }
                }
            }
        }
    }
}
