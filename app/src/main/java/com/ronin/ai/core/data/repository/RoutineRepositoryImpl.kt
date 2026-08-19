package com.ronin.ai.core.data.repository

import com.ronin.ai.core.data.db.dao.RoutineDao
import com.ronin.ai.core.data.db.dao.RoutineHistoryDao
import com.ronin.ai.core.data.db.entity.RoutineHistoryEntity
import com.ronin.ai.core.data.db.entity.toDomain
import com.ronin.ai.core.data.db.entity.toEntity
import com.ronin.ai.core.domain.model.Routine
import com.ronin.ai.core.domain.model.RoutineHistoryEntry
import com.ronin.ai.core.domain.model.RoutineRunStatus
import com.ronin.ai.core.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val historyDao: RoutineHistoryDao
) : RoutineRepository {

    override fun routines(): Flow<List<Routine>> =
        routineDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun history(): Flow<List<RoutineHistoryEntry>> =
        historyDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun historyFor(routineId: Long): Flow<List<RoutineHistoryEntry>> =
        historyDao.observeForRoutine(routineId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRoutine(id: Long): Routine? =
        routineDao.getById(id)?.toDomain()

    override suspend fun saveRoutine(routine: Routine): Long =
        routineDao.insert(routine.toEntity())

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        routineDao.setEnabled(id, enabled)
    }

    override suspend fun deleteRoutine(id: Long) {
        routineDao.deleteById(id)
        historyDao.deleteForRoutine(id)
    }

    override suspend fun recordRun(
        routineId: Long,
        status: RoutineRunStatus,
        detail: String
    ) {
        val entity = routineDao.getById(routineId) ?: return
        historyDao.insert(
            RoutineHistoryEntity(
                routineId = routineId,
                routineName = entity.name,
                status = status.name,
                detail = detail,
                executedAt = System.currentTimeMillis()
            )
        )
        routineDao.update(
            entity.copy(
                lastRunAt = System.currentTimeMillis(),
                runCount = entity.runCount + 1
            )
        )
    }

    override suspend fun count(): Int = routineDao.count()

    override suspend fun findRoutinesMatchingTrigger(input: String): List<Routine> {
        val lower = input.lowercase()
        return routineDao.observeEnabledWithTrigger().first()
            .map { it.toDomain() }
            .filter { routine ->
                routine.triggerPhrase.isNotBlank() && lower.contains(routine.triggerPhrase.lowercase())
            }
    }
}
