package com.ronin.ai.core.domain.model

/** Intent taxonomy used by the reasoning pipeline. */
enum class IntentType {
    OPEN_APP,
    BROWSER_SEARCH,
    BATTERY_STATUS,
    DEVICE_INFO,
    CREATE_NOTE,
    SEND_NOTIFICATION,
    RUN_ROUTINE,
    SAVE_MEMORY,
    RECALL_MEMORY,
    DEVICE_CONTROL,
    TIME_INFO,
    GENERAL
}

data class IntentMatch(
    val type: IntentType,
    val param: String = "",
    val confidence: Float = 0.8f
)

/**
 * Public reasoning stages. RONIN never exposes its internal chain of
 * thought — the UI only shows these high-level stage labels.
 */
enum class PipelineStage(val label: String) {
    UNDERSTANDING("Understanding input"),
    INTENT("Detecting intent"),
    PLANNING("Planning"),
    TOOL_SELECTION("Selecting tools"),
    EXECUTION("Executing"),
    EVALUATION("Evaluating result"),
    MEMORY_UPDATE("Updating memory")
}

data class ToolResult(
    val success: Boolean,
    val message: String,
    val intentType: IntentType? = null,
    val data: String? = null
)

/** Final output of one pipeline run. */
data class ChatReply(
    val reply: String,
    val toolUsed: String? = null,
    val provider: String? = null,
    val stages: List<PipelineStage> = emptyList()
)

enum class ToolCategory(val label: String) {
    APP_LAUNCHER("App launcher"),
    BROWSER("Browser"),
    NOTIFICATION("Notifications"),
    BATTERY("Battery"),
    DEVICE("Device"),
    FILES("Files"),
    AUTOMATION("Automation"),
    MEMORY("Memory"),
    SYSTEM("System")
}

/** Public description of a tool, used by the Skills screen. */
data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolCategory,
    val enabled: Boolean = true
)
