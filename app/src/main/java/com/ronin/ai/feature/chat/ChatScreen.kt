package com.ronin.ai.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.design.components.ChatBubble
import com.ronin.ai.core.design.components.EmptyState
import com.ronin.ai.core.design.components.ErrorBanner
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.LoadingDots
import com.ronin.ai.core.design.components.OptionChip
import com.ronin.ai.core.design.components.PipelineStageRow
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.RoninTextField
import com.ronin.ai.core.design.theme.RoninCyan

private val suggestions = listOf(
    "Open Spotify",
    "Check battery",
    "What do you remember about me?",
    "Remind me to drink water in 10 minutes",
    "Device info",
    "Run routine good morning"
)

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    RoninBackground {
        Column(Modifier.fillMaxSize()) {
            RoninHeader(
                title = "RONIN AI",
                subtitle = "assistant",
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearConversation) {
                            Icon(
                                Icons.Rounded.ClearAll,
                                contentDescription = "Clear conversation",
                                tint = RoninCyan
                            )
                        }
                    }
                }
            )

            if (error != null) {
                ErrorBanner(
                    message = error ?: "",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (messages.isEmpty() && !isThinking) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                EmptyState(
                                    icon = Icons.Rounded.ChatBubble,
                                    title = "Talk to RONIN",
                                    subtitle = "Ask questions, control your device, or try one of these:"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    suggestions.take(3).forEach { s ->
                                        OptionChip(s, false, { viewModel.onInputChange(s) })
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    suggestions.drop(3).forEach { s ->
                                        OptionChip(s, false, { viewModel.onInputChange(s) })
                                    }
                                }
                            }
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message)
                    }
                    if (isThinking) {
                        item {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (stage != null) {
                                    PipelineStageRow(stage?.label)
                                } else {
                                    LoadingDots()
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoninTextField(
                    value = input,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = "Message RONIN…",
                    singleLine = false
                )
                GradientButton(
                    text = "",
                    onClick = viewModel::send,
                    icon = Icons.Rounded.Send,
                    enabled = input.isNotBlank() && !isThinking,
                    modifier = Modifier.size(54.dp)
                )
            }
        }
    }
}
