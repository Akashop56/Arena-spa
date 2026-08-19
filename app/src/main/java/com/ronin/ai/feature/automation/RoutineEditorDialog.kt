package com.ronin.ai.feature.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.RoninTextField
import com.ronin.ai.core.design.components.SwitchRow
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.domain.model.Routine
import com.ronin.ai.core.domain.model.RoutineAction
import com.ronin.ai.core.domain.model.RoutineActionType

@Composable
fun RoutineEditorDialog(
    initial: Routine?,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, List<RoutineAction>, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var trigger by remember { mutableStateOf(initial?.triggerPhrase ?: "") }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var actions by remember { mutableStateOf(initial?.actions ?: emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New routine" else "Edit routine") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoninTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    placeholder = "e.g. Good morning",
                    singleLine = true
                )
                RoninTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = "Trigger phrase (optional)",
                    placeholder = "e.g. good morning",
                    singleLine = true
                )
                SwitchRow(
                    title = "Enabled",
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )

                Text("ACTIONS", style = MaterialTheme.typography.labelLarge, color = RoninCyan)

                actions.forEachIndexed { index, action ->
                    ActionEditorRow(
                        action = action,
                        onChange = { updated -> actions = actions.toMutableList().also { it[index] = updated } },
                        onRemove = { actions = actions.toMutableList().also { it.removeAt(index) } }
                    )
                }

                TextButton(onClick = {
                    actions = actions + RoutineAction(RoutineActionType.OPEN_APP, "")
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = RoninCyan)
                    Text("Add action", color = RoninCyan, modifier = Modifier.padding(start = 4.dp))
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = "Save",
                onClick = { onSave(initial?.id, name, trigger, actions, enabled) },
                enabled = name.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = RoninError) }
        }
    )
}

@Composable
private fun ActionEditorRow(
    action: RoutineAction,
    onChange: (RoutineAction) -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { menuOpen = true }) {
                Text(action.type.label, color = RoninCyan)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = RoninCyan)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                RoutineActionType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            onChange(action.copy(type = type))
                            menuOpen = false
                        }
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove action", tint = RoninError)
            }
        }
        RoninTextField(
            value = action.value,
            onValueChange = { onChange(action.copy(value = it)) },
            placeholder = action.type.placeholder,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
