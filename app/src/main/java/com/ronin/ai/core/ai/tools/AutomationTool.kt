package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.automation.AutomationEngine
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import com.ronin.ai.core.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Runs an automation routine by name, e.g. “run routine good morning”. */
@Singleton
class AutomationTool @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val automationEngine: AutomationEngine
) : RoninTool {

    override val definition = ToolDefinition(
        id = "automation",
        name = "Automation",
        description = "Run a saved routine, e.g. “run routine good morning”.",
        category = ToolCategory.AUTOMATION
    )

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.RUN_ROUTINE

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult {
        val query = param.trim().lowercase()
        if (query.isBlank()) {
            return ToolResult(false, "Which routine should I run?", IntentType.RUN_ROUTINE)
        }
        val routine = routineRepository.routines().first()
            .firstOrNull { it.name.lowercase().contains(query) }
        if (routine == null) {
            return ToolResult(false, "No routine named “$param” found.", IntentType.RUN_ROUTINE)
        }
        if (!routine.enabled) {
            return ToolResult(false, "Routine “${routine.name}” is disabled.", IntentType.RUN_ROUTINE)
        }
        val result = automationEngine.execute(routine)
        val message = "Routine “${routine.name}” — ${result.summary()}"
        return ToolResult(
            success = result.failedActions == 0 && result.actions > 0,
            message = message,
            intentType = IntentType.RUN_ROUTINE
        )
    }
}
