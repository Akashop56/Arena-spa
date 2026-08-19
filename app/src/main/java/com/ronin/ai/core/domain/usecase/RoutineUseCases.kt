package com.ronin.ai.core.domain.usecase

import com.ronin.ai.core.automation.AutomationEngine
import com.ronin.ai.core.domain.model.Routine
import com.ronin.ai.core.domain.model.RoutineAction
import com.ronin.ai.core.domain.model.RoutineHistoryEntry
import com.ronin.ai.core.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Routine lifecycle: create, enable/disable, delete, run, history. */
@Singleton
class RoutineUseCases @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val automationEngine: AutomationEngine
) {

    fun routines(): Flow<List<Routine>> = routineRepository.routines()

    fun history(): Flow<List<RoutineHistoryEntry>> = routineRepository.history()

    suspend fun save(
        id: Long?,
        name: String,
        triggerPhrase: String,
        actions: List<RoutineAction>,
        enabled: Boolean
    ): Long {
        val existing = id?.let { routineRepository.getRoutine(it) }
        val routine = Routine(
            id = id ?: 0L,
            name = name.trim().ifBlank { "Routine ${System.currentTimeMillis() % 10000}" },
            triggerPhrase = triggerPhrase.trim(),
            actions = actions,
            enabled = enabled,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            lastRunAt = existing?.lastRunAt,
            runCount = existing?.runCount ?: 0
        )
        return routineRepository.saveRoutine(routine)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        routineRepository.setEnabled(id, enabled)
    }

    suspend fun delete(id: Long) {
        routineRepository.deleteRoutine(id)
    }

    suspend fun runNow(id: Long) {
        val routine = routineRepository.getRoutine(id) ?: return
        automationEngine.execute(routine)
    }
}
