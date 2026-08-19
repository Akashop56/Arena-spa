package com.ronin.ai.feature.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.SectionHeader
import com.ronin.ai.core.design.components.StatusChip
import com.ronin.ai.core.design.components.SwitchRow
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninViolet
import com.ronin.ai.core.design.theme.RoninWarning
import com.ronin.ai.core.domain.model.Routine
import com.ronin.ai.core.domain.model.RoutineAction
import com.ronin.ai.core.domain.model.RoutineActionType
import com.ronin.ai.core.domain.model.RoutineHistoryEntry
import com.ronin.ai.core.domain.model.RoutineRunStatus

@Composable
fun AutomationScreen(viewModel: AutomationViewModel = hiltViewModel()) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var showEditor by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }
    var pendingDelete by remember { mutableStateOf<Routine?>(null) }

    RoninBackground {
        Column(Modifier.fillMaxSize()) {
            RoninHeader(
                title = "Automation",
                subtitle = "routines",
                actions = {
                    IconButton(onClick = {
                        editingRoutine = null
                        showEditor = true
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "New routine", tint = RoninCyan)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SectionHeader(title = "Routines (${routines.size})") }

                if (routines.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.Bolt,
                            title = "No routines yet",
                            subtitle = "Create routines with multiple actions — open apps, send notifications, set volume and more. They can also be triggered from chat."
                        )
                    }
                } else {
                    items(routines, key = { it.id }) { routine ->
                        RoutineCard(
                            routine = routine,
                            onToggle = { viewModel.toggleEnabled(routine.id, it) },
                            onRun = { viewModel.runNow(routine.id) },
                            onEdit = {
                                editingRoutine = routine
                                showEditor = true
                            },
                            onDelete = { pendingDelete = routine }
                        )
                    }
                }

                item {
                    SectionHeader(title = "Execution history", modifier = Modifier.padding(top = 12.dp))
                }
                if (history.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.History,
                            title = "No runs yet",
                            subtitle = "Every execution is recorded here with its status."
                        )
                    }
                } else {
                    items(history.take(20), key = { it.id }) { entry ->
                        HistoryRow(entry)
                    }
                }
            }
        }
    }

    if (showEditor) {
        RoutineEditorDialog(
            initial = editingRoutine,
            onDismiss = {
                showEditor = false
                editingRoutine = null
            },
            onSave = { id, name, phrase, actions, enabled ->
                viewModel.save(id, name, phrase, actions, enabled)
                showEditor = false
                editingRoutine = null
            }
        )
    }

    pendingDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete routine?") },
            text = { Text("“${routine.name}” and its history will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(routine.id)
                    pendingDelete = null
                }) { Text("Delete", color = RoninError) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    onToggle: (Boolean) -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = routine.enabled) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    routine.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    if (routine.enabled) "ACTIVE" else "OFF",
                    if (routine.enabled) RoninSuccess else RoninTextSecondary
                )
            }
            if (routine.triggerPhrase.isNotBlank()) {
                Text(
                    "Trigger: “${routine.triggerPhrase}”",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoninViolet
                )
            }
            Text(
                routine.actions.joinToString("  ·  ") { it.type.label },
                style = MaterialTheme.typography.bodyMedium,
                color = RoninTextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildString {
                        append(TimeFormat.relative(routine.createdAt))
                        routine.lastRunAt?.let { append(" · last run ${TimeFormat.relative(it)}") }
                        if (routine.runCount > 0) append(" · ${routine.runCount} runs")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = RoninTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRun) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Run now", tint = RoninCyan)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = RoninCyan)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = RoninError)
                }
            }
            SwitchRow(
                title = "Enabled",
                checked = routine.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: RoutineHistoryEntry) {
    val color = when (entry.status) {
        RoutineRunStatus.SUCCESS -> RoninSuccess
        RoutineRunStatus.PARTIAL -> RoninWarning
        RoutineRunStatus.FAILED -> RoninError
    }
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.routineName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(entry.detail, style = MaterialTheme.typography.bodyMedium, color = RoninTextSecondary)
                Text(
                    TimeFormat.dateTime(entry.executedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = RoninTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            StatusChip(entry.status.label.uppercase(), color)
        }
    }
}
