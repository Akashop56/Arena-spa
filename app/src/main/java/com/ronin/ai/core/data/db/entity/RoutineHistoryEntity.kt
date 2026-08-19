package com.ronin.ai.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ronin.ai.core.domain.model.RoutineHistoryEntry
import com.ronin.ai.core.domain.model.RoutineRunStatus

@Entity(
    tableName = "routine_history",
    indices = [Index("routineId"), Index("executedAt")]
)
data class RoutineHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long,
    val routineName: String,
    val status: String,
    val detail: String,
    val executedAt: Long
)

fun RoutineHistoryEntity.toDomain(): RoutineHistoryEntry = RoutineHistoryEntry(
    id = id,
    routineId = routineId,
    routineName = routineName,
    status = RoutineRunStatus.valueOf(status),
    detail = detail,
    executedAt = executedAt
)

fun RoutineHistoryEntry.toEntity(): RoutineHistoryEntity = RoutineHistoryEntity(
    id = id,
    routineId = routineId,
    routineName = routineName,
    status = status.name,
    detail = detail,
    executedAt = executedAt
)
