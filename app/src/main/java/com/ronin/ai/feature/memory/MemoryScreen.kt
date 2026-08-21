package com.ronin.ai.feature.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.common.TimeFormat
import com.ronin.ai.core.design.components.EmptyState
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.OptionChip
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.RoninTextField
import com.ronin.ai.core.design.components.StatusChip
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninAmber
import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType

@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MemoryItem?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    RoninBackground {
        Column(Modifier.fillMaxSize()) {
            RoninHeader(
                title = "Memory",
                subtitle = "brain storage",
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add memory", tint = RoninCyan)
                    }
                }
            )

            RoninTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = "Search memories…",
                leadingIcon = Icons.Rounded.Search,
                singleLine = true
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OptionChip("All", filter == null, { viewModel.setFilter(null) })
                }
                items(MemoryType.entries.toList()) { type ->
                    OptionChip(type.label, filter == type, { viewModel.setFilter(type) })
                }
            }

            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Memory,
                    title = if (query.isNotBlank()) "No results for “$query”" else "No memories yet",
                    subtitle = "Tell RONIN “remember that …” in chat, or add one manually."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        MemoryCard(
                            item = item,
                            onEdit = { editing = item },
                            onDelete = { viewModel.delete(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || editing != null) {
        MemoryEditorDialog(
            initial = editing,
            onDismiss = {
                showAddDialog = false
                editing = null
            },
            onSave = { type, title, content ->
                val now = System.currentTimeMillis()
                val base = editing
                viewModel.save(
                    MemoryItem(
                        id = base?.id ?: 0L,
                        type = type,
                        title = title,
                        content = content,
                        source = base?.source ?: "manual",
                        createdAt = base?.createdAt ?: now,
                        updatedAt = now,
                        importance = base?.importance ?: 1
                    )
                )
                showAddDialog = false
                editing = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all memory?") },
            text = { Text("This deletes every stored memory, preference and learned solution. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearConfirm = false
                }) { Text("Clear all", color = RoninError) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MemoryCard(
    item: MemoryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    item.type.label,
                    when (item.type) {
                        MemoryType.PREFERENCE -> RoninAmber
                        MemoryType.LEARNED_SOLUTION -> RoninSuccess
                        MemoryType.CONVERSATION -> RoninCyan
                        else -> RoninCyan
                    }
                )
            }
            Text(item.content, style = MaterialTheme.typography.bodyMedium, color = RoninTextSecondary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    TimeFormat.relative(item.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = RoninTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.padding(0.dp)) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = RoninCyan,
                        modifier = Modifier.padding(0.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.padding(0.dp)) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete",
                        tint = RoninError
                    )
                }
            }
        }
    }
}
