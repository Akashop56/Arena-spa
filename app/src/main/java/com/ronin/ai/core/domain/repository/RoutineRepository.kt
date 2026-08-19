package com.ronin.ai.core.domain.repository

import com.ronin.ai.core.domain.model.Routine
import com.ronin.ai.core.domain.model.RoutineAction
import com.ronin.ai.core.domain.model.RoutineHistoryEntry
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun routines(): Flow<List<Routine>>
    fun history(): Flow<List<RoutineHistoryEntry>>
    fun historyFor(routineId: Long): Flow<List<RoutineHistoryEntry>>
    suspend fun getRoutine(id: Long): Routine?
    suspend fun saveRoutine(routine: Routine): Long
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun deleteRoutine(id: Long)
    suspend fun recordRun(routineId: Long, status: com.ronin.ai.core.domain.model.RoutineRunStatus, detail: String)
    suspend fun count(): Int
    suspend fun findRoutinesMatchingTrigger(input: String): List<Routine>
}
