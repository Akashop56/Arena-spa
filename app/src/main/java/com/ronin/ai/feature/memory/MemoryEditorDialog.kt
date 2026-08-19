package com.ronin.ai.feature.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.OptionChip
import com.ronin.ai.core.design.components.RoninTextField
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType

@Composable
fun MemoryEditorDialog(
    initial: MemoryItem?,
    onDismiss: () -> Unit,
    onSave: (MemoryType, String, String) -> Unit
) {
    var type by remember { mutableStateOf(initial?.type ?: MemoryType.LONG_TERM) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New memory" else "Edit memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MemoryType.entries.toList()) { t ->
                        OptionChip(t.label, t == type, onClick = { type = t })
                    }
                }
                RoninTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title",
                    singleLine = true
                )
                RoninTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = "Content",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GradientButton(
                text = "Save",
                onClick = {
                    if (content.isNotBlank()) {
                        onSave(type, title.ifBlank { content.take(40) }, content)
                    }
                },
                enabled = content.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = RoninError) }
        }
    )
}
