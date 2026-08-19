package com.ronin.ai.core.automation

import android.content.Context
import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.device.NotificationCenter
import com.ronin.ai.core.domain.model.Routine
import com.ronin.ai.core.domain.model.RoutineAction
import com.ronin.ai.core.domain.model.RoutineActionType
import com.ronin.ai.core.domain.model.RoutineRunStatus
import com.ronin.ai.core.domain.repository.RoutineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class RoutineExecutionResult(
    val actions: Int,
    val failedActions: Int,
    val details: List<String>
) {
    fun summary(): String = when {
        actions == 0 -> "no actions defined"
        failedActions == 0 -> "completed ${actions} action(s)"
        failedActions == actions -> "failed (${failedActions}/${actions})"
        else -> "partially completed ($failedActions/$actions failed)"
    }
}

/**
 * Executes routine action lists against the device layer and records each
 * run into the routine history.
 */
@Singleton
class AutomationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceManager: DeviceManager,
    private val notificationCenter: NotificationCenter,
    private val routineRepository: RoutineRepository
) {

    private val notesDir = File(context.filesDir, "ronin_notes").apply { mkdirs() }

    suspend fun execute(routine: Routine): RoutineExecutionResult =
        withContext(Dispatchers.IO) {
            val details = mutableListOf<String>()
            var failed = 0
            for (action in routine.actions) {
                val ok = executeAction(routine, action)
                details += "${action.type.label}: ${if (ok) "ok" else "failed"}"
                if (!ok) failed++
            }
            val result = RoutineExecutionResult(
                actions = routine.actions.size,
                failedActions = failed,
                details = details
            )
            val status = when {
                routine.actions.isEmpty() -> RoutineRunStatus.FAILED
                failed == 0 -> RoutineRunStatus.SUCCESS
                failed == routine.actions.size -> RoutineRunStatus.FAILED
                else -> RoutineRunStatus.PARTIAL
            }
            routineRepository.recordRun(routine.id, status, result.summary())
            result
        }

    private suspend fun executeAction(routine: Routine, action: RoutineAction): Boolean =
        runCatching {
            when (action.type) {
                RoutineActionType.OPEN_APP -> openApp(action.value)
                RoutineActionType.BROWSER_SEARCH -> openSearch(action.value)
                RoutineActionType.SEND_NOTIFICATION -> {
                    notificationCenter.post(
                        title = routine.name,
                        text = action.value.ifBlank { "RONIN routine executed" }
                    )
                    true
                }
                RoutineActionType.SET_VOLUME -> {
                    val percent = action.value.toIntOrNull()?.coerceIn(0, 100) ?: return@runCatching false
                    deviceManager.setVolume(percent * deviceManager.maxVolume / 100)
                }
                RoutineActionType.TOGGLE_TORCH -> {
                    when (action.value.lowercase()) {
                        "on" -> deviceManager.toggleTorch(true)
                        "off" -> deviceManager.toggleTorch(false)
                        else -> deviceManager.toggleTorch(!deviceManager.isTorchOn())
                    }
                }
                RoutineActionType.CREATE_NOTE -> {
                    if (action.value.isBlank()) return@runCatching false
                    val file = File(
                        notesDir,
                        "routine-${System.currentTimeMillis()}.txt"
                    )
                    file.writeText(action.value)
                    true
                }
            }
        }.getOrDefault(false)

    private fun openApp(query: String): Boolean {
        if (query.isBlank()) return false
        val q = query.lowercase()
        val app = deviceManager.getInstalledApps()
            .filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
            .minByOrNull { it.label.length }
            ?: return false
        return deviceManager.launchApp(app.packageName)
    }

    private fun openSearch(query: String): Boolean {
        if (query.isBlank()) return false
        return deviceManager.openUrl(
            "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
        )
    }
}
