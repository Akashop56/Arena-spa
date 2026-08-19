package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Summarises hardware + system information. */
@Singleton
class DeviceInfoTool @Inject constructor(
    private val deviceManager: DeviceManager
) : RoninTool {

    override val definition = ToolDefinition(
        id = "device_info",
        name = "Device information",
        description = "Report device model, Android version, storage and RAM.",
        category = ToolCategory.DEVICE
    )

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.DEVICE_INFO

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val info = deviceManager.getDeviceInfo()
        val message = buildString {
            appendLine("📱 ${info.brand} ${info.model} (${info.manufacturer})")
            appendLine("Android ${info.androidVersion} · API ${info.sdkInt}")
            appendLine("Chipset ABI: ${info.cpuAbi}")
            appendLine(
                "Storage: ${formatBytes(info.storageTotalBytes - info.storageFreeBytes)} used of " +
                    "${formatBytes(info.storageTotalBytes)}"
            )
            append(
                "RAM: ${formatBytes(info.ramTotalBytes - info.ramAvailableBytes)} used of " +
                    "${formatBytes(info.ramTotalBytes)}"
            )
        }
        return ToolResult(true, message, IntentType.DEVICE_INFO)
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) {
            String.format(Locale.US, "%.1f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.0f MB", mb)
        }
    }
}
