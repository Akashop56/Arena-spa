package com.ronin.ai.feature.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.design.components.EmptyState
import com.ronin.ai.core.design.components.GlowOrb
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.SectionHeader
import com.ronin.ai.core.design.components.StatTile
import com.ronin.ai.core.design.components.StatusChip
import com.ronin.ai.core.design.navigation.RoninDestination
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninAmber
import com.ronin.ai.core.design.theme.RoninWarning
import com.ronin.ai.core.domain.model.DashboardData
import com.ronin.ai.core.design.components.ChatBubble

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val data by viewModel.data.collectAsStateWithLifecycle()

    // Ask for notification permission once (Android 13+).
    var askedNotificationPermission by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !askedNotificationPermission) {
            askedNotificationPermission = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    RoninBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { DashboardHeader(data) }
            item { ProviderStatusCard(data, onOpenProviders = { onNavigate(RoninDestination.AI_PROVIDERS.route) }) }
            item { StatsGrid(data, onOpenSkills = { onNavigate(RoninDestination.SKILLS.route) }) }
            item { QuickActions(onNavigate) }
            item {
                SectionHeader(title = "Recent conversation")
            }
            if (data.recentMessages.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.ChatBubble,
                        title = "No conversation yet",
                        subtitle = "Ask me anything — or tap Chat and say hello."
                    )
                }
            } else {
                items(data.recentMessages.take(3)) { message ->
                    ChatBubble(message)
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(data: DashboardData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting(),
                style = MaterialTheme.typography.titleMedium,
                color = RoninTextSecondary
            )
            Text(
                text = "${data.assistantName} AI",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        GlowOrb(modifier = Modifier.size(64.dp))
    }
}

@Composable
private fun ProviderStatusCard(data: DashboardData, onOpenProviders: () -> Unit) {
    val config = data.defaultProvider
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = RoninCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "AI BRAIN",
                    style = MaterialTheme.typography.labelLarge,
                    color = RoninCyan,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.weight(1f))
                if (config != null && config.enabled && config.hasKey) {
                    StatusChip("ONLINE", RoninSuccess)
                } else {
                    StatusChip("NOT CONNECTED", RoninWarning)
                }
            }
            if (config != null) {
                Text(
                    "${config.type.displayName} · ${config.effectiveModel}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (config.hasKey) "API key: stored securely ✓" else "No API key set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoninTextSecondary
                )
            }
            TextButton(onClick = onOpenProviders, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Rounded.Key, contentDescription = null, tint = RoninCyan, modifier = Modifier.size(16.dp))
                Text(
                    "Configure providers",
                    color = RoninCyan,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(data: DashboardData, onOpenSkills: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            icon = Icons.Rounded.Memory,
            value = data.memoryCount.toString(),
            label = "Memories",
            modifier = Modifier.weight(1f),
            accent = RoninCyan
        )
        StatTile(
            icon = Icons.Rounded.Bolt,
            value = data.routineCount.toString(),
            label = "Routines",
            modifier = Modifier.weight(1f),
            accent = RoninAmber
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            icon = Icons.Rounded.Psychology,
            value = data.skillCount.toString(),
            label = "Skills",
            modifier = Modifier.weight(1f),
            accent = RoninAmber
        )
        StatTile(
            icon = Icons.Rounded.BatteryFull,
            value = if (data.battery != null) "${data.battery.level}%" else "—",
            label = if (data.battery?.isCharging == true) "Charging" else "Battery",
            modifier = Modifier.weight(1f),
            accent = if ((data.battery?.level ?: 100) <= 20) RoninWarning else RoninCyan
        )
    }
}

@Composable
private fun QuickActions(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Quick actions")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionButton(
                label = "Chat",
                icon = Icons.Rounded.ChatBubble,
                accent = RoninCyan,
                modifier = Modifier.weight(1f)
            ) { onNavigate(RoninDestination.CHAT.route) }
            QuickActionButton(
                label = "Voice",
                icon = Icons.Rounded.Mic,
                accent = RoninAmber,
                modifier = Modifier.weight(1f)
            ) { onNavigate(RoninDestination.VOICE.route) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionButton(
                label = "Memory",
                icon = Icons.Rounded.Memory,
                accent = RoninCyan,
                modifier = Modifier.weight(1f)
            ) { onNavigate(RoninDestination.MEMORY.route) }
            QuickActionButton(
                label = "Automation",
                icon = Icons.Rounded.Bolt,
                accent = RoninAmber,
                modifier = Modifier.weight(1f)
            ) { onNavigate(RoninDestination.AUTOMATION.route) }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GradientButton(
        text = label,
        onClick = onClick,
        icon = icon,
        modifier = modifier
    )
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Working late"
    }
}
