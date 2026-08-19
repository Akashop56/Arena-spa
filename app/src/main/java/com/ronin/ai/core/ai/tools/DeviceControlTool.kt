package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/** Volume, torch and brightness control via natural language. */
@Singleton
class DeviceControlTool @Inject constructor(
    private val deviceManager: DeviceManager
) : RoninTool {

    override val definition = ToolDefinition(
        id = "device_control",
        name = "Device control",
        description = "Adjust volume, torch and brightness, e.g. “volume up”, “torch on”, “brightness 50”.",
        category = ToolCategory.DEVICE
    )

    private val volumeUp = Regex("""volume\s+up""")
    private val volumeDown = Regex("""volume\s+down""")
    private val volumeSet = Regex("""volume\s+(?:to\s+)?(\d{1,3})""")
    private val torchOn = Regex("""(?:torch|flashlight)\s+on""")
    private val torchOff = Regex("""(?:torch|flashlight)\s+off""")
    private val brightnessSet = Regex("""brightness\s+(?:to\s+)?(\d{1,3})""")

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.DEVICE_CONTROL

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val text = input.lowercase()

        volumeSet.find(text)?.let { m ->
            val percent = m.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: return@let
            val level = percent * deviceManager.maxVolume / 100
            return if (deviceManager.setVolume(level)) {
                ToolResult(true, "Volume set to $percent%.", IntentType.DEVICE_CONTROL)
            } else {
                ToolResult(false, "Couldn't change the volume.", IntentType.DEVICE_CONTROL)
            }
        }
        when {
            volumeUp.containsMatchIn(text) -> {
                val ok = deviceManager.adjustVolume(+1)
                return ToolResult(
                    ok,
                    if (ok) "Volume up." else "Couldn't change the volume.",
                    IntentType.DEVICE_CONTROL
                )
            }
            volumeDown.containsMatchIn(text) -> {
                val ok = deviceManager.adjustVolume(-1)
                return ToolResult(
                    ok,
                    if (ok) "Volume down." else "Couldn't change the volume.",
                    IntentType.DEVICE_CONTROL
                )
            }
        }

        brightnessSet.find(text)?.let { m ->
            val percent = m.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: return@let
            return if (deviceManager.setBrightness(percent)) {
                ToolResult(true, "Brightness set to $percent%.", IntentType.DEVICE_CONTROL)
            } else {
                ToolResult(
                    false,
                    "Brightness needs the “Modify system settings” permission — enable it in Device Control.",
                    IntentType.DEVICE_CONTROL
                )
            }
        }

        when {
            torchOn.containsMatchIn(text) -> return toggleTorch(true)
            torchOff.containsMatchIn(text) -> return toggleTorch(false)
        }

        return ToolResult(false, "I can control volume, torch and brightness.", IntentType.DEVICE_CONTROL)
    }

    private fun toggleTorch(on: Boolean): ToolResult {
        val ok = deviceManager.toggleTorch(on)
        return ToolResult(
            ok,
            if (ok) "Torch ${if (on) "on" else "off"}." else "Torch isn't available on this device.",
            IntentType.DEVICE_CONTROL
        )
    }
}
