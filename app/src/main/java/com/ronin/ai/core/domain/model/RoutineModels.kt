package com.ronin.ai.core.domain.model

enum class RoutineActionType(val toolId: String, val label: String, val placeholder: String) {
    OPEN_APP("app_launcher", "Open app", "app name, e.g. Spotify"),
    BROWSER_SEARCH("browser", "Search the web", "query, e.g. weather today"),
    SEND_NOTIFICATION("notification", "Send notification", "message text"),
    SET_VOLUME("device_control", "Set volume", "0 - 100"),
    TOGGLE_TORCH("device_control", "Toggle torch", "on / off"),
    CREATE_NOTE("files", "Create note", "note text")
}

data class RoutineAction(val type: RoutineActionType, val value: String)

data class Routine(
    val id: Long = 0L,
    val name: String,
    val triggerPhrase: String = "",
    val actions: List<RoutineAction> = emptyList(),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val runCount: Int = 0
)

enum class RoutineRunStatus(val label: String) {
    SUCCESS("Success"),
    PARTIAL("Partial"),
    FAILED("Failed")
}

data class RoutineHistoryEntry(
    val id: Long = 0L,
    val routineId: Long,
    val routineName: String,
    val status: RoutineRunStatus,
    val detail: String,
    val executedAt: Long = System.currentTimeMillis()
)
