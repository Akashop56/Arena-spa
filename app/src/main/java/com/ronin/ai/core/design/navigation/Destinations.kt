package com.ronin.ai.core.design.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector

enum class RoninDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val inBottomBar: Boolean = true
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Rounded.Dashboard),
    CHAT("chat", "Chat", Icons.Rounded.ChatBubble),
    VOICE("voice", "Voice", Icons.Rounded.Mic),
    MEMORY("memory", "Memory", Icons.Rounded.Memory),
    DEVICE("device", "Device", Icons.Rounded.Smartphone),
    AUTOMATION("automation", "Automation", Icons.Rounded.Bolt),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings),
    SKILLS("skills", "Skills", Icons.Rounded.Psychology, inBottomBar = false),
    AI_PROVIDERS("ai_providers", "AI Providers", Icons.Rounded.Key, inBottomBar = false),
    AI_PROVIDER_EDIT("ai_provider_edit/{type}", "AI Provider", Icons.Rounded.Key, inBottomBar = false),
    VOICE_SETTINGS("voice_settings", "Voice", Icons.Rounded.RecordVoiceOver, inBottomBar = false);

    companion object {
        fun bottomBarDestinations(): List<RoninDestination> =
            entries.filter { it.inBottomBar }
    }
}

fun aiProviderEditRoute(type: String): String = "ai_provider_edit/$type"
