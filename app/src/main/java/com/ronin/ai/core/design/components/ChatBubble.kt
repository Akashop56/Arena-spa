package com.ronin.ai.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ronin.ai.core.common.TimeFormat
import com.ronin.ai.core.design.theme.RoninBlack
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninSurfaceHigh
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninViolet
import com.ronin.ai.core.domain.model.ChatMessage
import com.ronin.ai.core.domain.model.ChatRole

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == ChatRole.USER
    val shape = if (isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .let {
                    if (isUser) {
                        it
                            .background(Brush.horizontalGradient(listOf(RoninCyan, RoninViolet)), shape)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    } else {
                        it
                            .background(RoninSurfaceHigh, shape)
                            .border(1.dp, RoninCyan.copy(alpha = 0.22f), shape)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    }
                },
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) RoninBlack else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = buildString {
                    if (message.toolUsed != null) append("🔧 ${message.toolUsed} · ")
                    append(TimeFormat.clock(message.timestamp))
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isUser) RoninBlack.copy(alpha = 0.6f) else RoninTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
