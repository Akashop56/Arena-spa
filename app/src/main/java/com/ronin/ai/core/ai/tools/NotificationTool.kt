package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.device.NotificationCenter
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/** Sends RONIN notifications, optionally after a delay (“remind me in 10 minutes”). */
@Singleton
class NotificationTool @Inject constructor(
    private val notificationCenter: NotificationCenter
) : RoninTool {

    override val definition = ToolDefinition(
        id = "notification",
        name = "Notifications & reminders",
        description = "Send a notification or a delayed reminder, e.g. “remind me to drink water in 30 minutes”.",
        category = ToolCategory.NOTIFICATION
    )

    private val delayPattern = Regex("""in\s+(\d+)\s+(second|minute|hour)s?""")

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.SEND_NOTIFICATION

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val delayMatch = delayPattern.find(input.lowercase())
        val delayMillis = delayMatch?.let { m ->
            val amount = m.groupValues[1].toLongOrNull() ?: 0L
            when (m.groupValues[2]) {
                "second" -> amount * 1_000L
                "hour" -> amount * 3_600_000L
                else -> amount * 60_000L
            }
        } ?: 0L

        val message = param.trim().ifBlank {
            delayMatch?.let { input.replace(it.value, "").trim() } ?: ""
        }
        if (message.isBlank()) {
            return ToolResult(false, "What should I remind you about?", IntentType.SEND_NOTIFICATION)
        }

        val whenText = if (delayMillis > 0) " in ${delayText(delayMillis)}" else ""
        notificationCenter.post(
            title = "RONIN reminder",
            text = message,
            delayMillis = delayMillis
        )
        return ToolResult(
            true,
            "Done — I'll notify you about “$message”$whenText.",
            IntentType.SEND_NOTIFICATION
        )
    }

    private fun delayText(millis: Long): String {
        val minutes = millis / 60_000L
        return when {
            millis < 60_000L -> "${millis / 1_000L} seconds"
            minutes < 60L -> "$minutes minutes"
            else -> "${minutes / 60L} hours"
        }
    }
}
