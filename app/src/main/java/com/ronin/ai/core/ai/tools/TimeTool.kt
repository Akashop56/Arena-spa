package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.common.TimeFormat
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/** Reports the current time and date. */
@Singleton
class TimeTool @Inject constructor() : RoninTool {

    override val definition = ToolDefinition(
        id = "time",
        name = "Time & date",
        description = "Tell the current time and date.",
        category = ToolCategory.SYSTEM
    )

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.TIME_INFO

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val now = System.currentTimeMillis()
        return ToolResult(
            true,
            "It's ${TimeFormat.clock(now)} on ${TimeFormat.day(now)}.",
            IntentType.TIME_INFO
        )
    }
}
