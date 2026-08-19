package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.domain.model.IntentMatch
import com.ronin.ai.core.domain.model.ToolDefinition
import javax.inject.Inject
import javax.inject.Singleton

/** Holds every registered skill and resolves intents to tools. */
@Singleton
class ToolRegistry @Inject constructor(
    private val appLauncherTool: AppLauncherTool,
    private val browserTool: BrowserTool,
    private val notificationTool: NotificationTool,
    private val batteryTool: BatteryTool,
    private val deviceInfoTool: DeviceInfoTool,
    private val deviceControlTool: DeviceControlTool,
    private val fileTool: FileTool,
    private val automationTool: AutomationTool,
    private val timeTool: TimeTool,
    private val memoryTool: MemoryTool
) {

    val tools: List<RoninTool> = listOf(
        appLauncherTool,
        browserTool,
        notificationTool,
        batteryTool,
        deviceInfoTool,
        deviceControlTool,
        fileTool,
        automationTool,
        timeTool,
        memoryTool
    )

    fun definitions(): List<ToolDefinition> = tools.map { it.definition }

    fun findFor(intent: IntentMatch): RoninTool? =
        tools.firstOrNull { it.matches(intent.type, intent.param) }
}
