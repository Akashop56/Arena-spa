package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/** Reports battery state. */
@Singleton
class BatteryTool @Inject constructor(
    private val deviceManager: DeviceManager
) : RoninTool {

    override val definition = ToolDefinition(
        id = "battery",
        name = "Battery monitor",
        description = "Report battery level, charging state and temperature.",
        category = ToolCategory.BATTERY
    )

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.BATTERY_STATUS

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val battery = deviceManager.getBatteryState()
        val message = buildString {
            append("Battery at ${battery.level}%")
            append(
                if (battery.isCharging) " · charging (${battery.pluggedLabel})"
                else " · ${battery.statusLabel.lowercase()}"
            )
            append(" · ${battery.temperatureC}°C")
            append(" · health: ${battery.healthLabel.lowercase()}")
            if (battery.level <= 20) append("\n⚠️ Battery is low — consider charging.")
            else if (battery.level >= 95 && battery.isCharging) append("\nBattery is nearly full — you can unplug.")
        }
        return ToolResult(true, message, IntentType.BATTERY_STATUS)
    }
}
