package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult

/**
 * A modular skill. Each tool declares its capabilities via [definition]
 * and decides whether it can handle an intent in [matches].
 */
interface RoninTool {
    val definition: ToolDefinition

    fun matches(intent: IntentType, param: String): Boolean

    suspend fun execute(intent: IntentType, param: String, input: String): ToolResult
}
