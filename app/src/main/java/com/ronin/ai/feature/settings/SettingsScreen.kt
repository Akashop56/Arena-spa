package com.ronin.ai.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.common.Constants
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.RoninTextField
import com.ronin.ai.core.design.components.SectionHeader
import com.ronin.ai.core.design.components.SettingsRow
import com.ronin.ai.core.design.components.SwitchRow
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninAmber

@Composable
fun SettingsScreen(
    onOpenAiProviders: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenSkills: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val assistantName by viewModel.assistantName.collectAsStateWithLifecycle()
    val speechOutput by viewModel.speechOutput.collectAsStateWithLifecycle()
    val defaultProvider by viewModel.defaultProvider.collectAsStateWithLifecycle()
    val voiceProvider by viewModel.voiceProvider.collectAsStateWithLifecycle()
    val memoryEnabled by viewModel.memoryEnabled.collectAsStateWithLifecycle()

    // Reading this on every recomposition hit the PackageManager repeatedly;
    // refresh it once per resume instead.
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationAccess by remember { mutableStateOf(viewModel.isNotificationAccessGranted()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccess = viewModel.isNotificationAccessGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showNameDialog by remember { mutableStateOf(false) }
    var showClearMemoryConfirm by remember { mutableStateOf(false) }
    var showClearExperienceConfirm by remember { mutableStateOf(false) }

    RoninBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { RoninHeader(title = "Settings") }

            item { SectionHeader(title = "Assistant") }
            item {
                SettingsRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Assistant name",
                    subtitle = assistantName,
                    onClick = { showNameDialog = true }
                )
            }
            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                    SwitchRow(
                        title = "Speak replies aloud",
                        subtitle = "Auto-play assistant replies with TTS",
                        checked = speechOutput,
                        onCheckedChange = viewModel::setSpeechOutput
                    )
                }
            }

            item { SectionHeader(title = "AI brain", modifier = Modifier.padding(top = 8.dp)) }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Key,
                    title = "AI Providers",
                    subtitle = "Default: ${defaultProvider.displayName}",
                    onClick = onOpenAiProviders
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.RecordVoiceOver,
                    title = "Voice",
                    subtitle = voiceProvider.displayName,
                    onClick = onOpenVoiceSettings
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Psychology,
                    title = "Skills",
                    subtitle = "Tools & learned solutions",
                    onClick = onOpenSkills
                )
            }

            item { SectionHeader(title = "Memory", modifier = Modifier.padding(top = 8.dp)) }
            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                    SwitchRow(
                        title = "Memory engine",
                        subtitle = if (memoryEnabled) {
                            "RONIN remembers preferences and recalls them in chat"
                        } else {
                            "Paused — nothing is stored or recalled"
                        },
                        checked = memoryEnabled,
                        onCheckedChange = viewModel::setMemoryEnabled
                    )
                }
            }

            item { SectionHeader(title = "Security & data", modifier = Modifier.padding(top = 8.dp)) }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Security,
                    title = "API key storage",
                    subtitle = "Encrypted with Android Keystore (AES-256-GCM)",
                    onClick = { }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Notification access",
                    subtitle = if (notificationAccess) "Granted" else "Not granted",
                    onClick = viewModel::openNotificationAccessSettings
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Clear all memory",
                    subtitle = "Erase memories, preferences & learned solutions",
                    onClick = { showClearMemoryConfirm = true }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Terminal,
                    title = "Clear experience log",
                    subtitle = "Erase recorded errors and fixes",
                    onClick = { showClearExperienceConfirm = true }
                )
            }

            item { SectionHeader(title = "About", modifier = Modifier.padding(top = 8.dp)) }
            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("RONIN AI", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Version ${Constants.APP_VERSION} · Clean Architecture · Compose · Room · Hilt",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoninTextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        NameDialog(
            initial = assistantName,
            onDismiss = { showNameDialog = false },
            onSave = {
                viewModel.setAssistantName(it)
                showNameDialog = false
            }
        )
    }

    if (showClearMemoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearMemoryConfirm = false },
            title = { Text("Clear all memory?") },
            text = { Text("All memories, preferences and learned solutions will be erased.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllMemory()
                    showClearMemoryConfirm = false
                }) { Text("Clear", color = RoninError) }
            },
            dismissButton = {
                TextButton(onClick = { showClearMemoryConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearExperienceConfirm) {
        AlertDialog(
            onDismissRequest = { showClearExperienceConfirm = false },
            title = { Text("Clear experience log?") },
            text = { Text("Recorded errors and successful fixes will be erased. RONIN will start learning from scratch.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearExperiences()
                    showClearExperienceConfirm = false
                }) { Text("Clear", color = RoninError) }
            },
            dismissButton = {
                TextButton(onClick = { showClearExperienceConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assistant name") },
        text = {
            RoninTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Save", color = RoninCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
