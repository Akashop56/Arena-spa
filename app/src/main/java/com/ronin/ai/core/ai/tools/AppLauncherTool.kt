package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.device.AppInfo
import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/** Launches installed apps by name or package. */
@Singleton
class AppLauncherTool @Inject constructor(
    private val deviceManager: DeviceManager
) : RoninTool {

    override val definition = ToolDefinition(
        id = "app_launcher",
        name = "App launcher",
        description = "Open any installed app by name, e.g. “open Spotify”.",
        category = ToolCategory.APP_LAUNCHER
    )

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.OPEN_APP

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val query = param.trim().lowercase()
        if (query.isBlank()) {
            return ToolResult(false, "Which app should I open?", IntentType.OPEN_APP)
        }
        val apps = deviceManager.getInstalledApps()

        fun matchScore(app: AppInfo): Int {
            val label = app.label.lowercase()
            val pkg = app.packageName.lowercase()
            return when {
                label == query -> 3
                label.contains(query) -> 2
                pkg.contains(query) -> 1
                else -> 0
            }
        }

        val best = apps.map { it to matchScore(it) }.filter { it.second > 0 }
            .maxByOrNull { it.second }?.first

        if (best == null) {
            val suggestions = apps
                .filter { it.label.lowercase().startsWith(query.firstOrNull() ?: ' ') }
                .take(3)
                .joinToString(", ") { it.label }
            val hint = if (suggestions.isNotBlank()) " Did you mean: $suggestions?" else ""
            return ToolResult(
                false,
                "I couldn't find an app called “$param”.$hint",
                IntentType.OPEN_APP
            )
        }

        val launched = deviceManager.launchApp(best.packageName)
        return if (launched) {
            ToolResult(true, "Opened ${best.label}.", IntentType.OPEN_APP, best.packageName)
        } else {
            ToolResult(false, "I found ${best.label} but couldn't launch it.", IntentType.OPEN_APP)
        }
    }
}
