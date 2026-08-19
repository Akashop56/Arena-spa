package com.ronin.ai.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ronin.ai.core.domain.model.Routine
import com.ronin.ai.core.domain.model.RoutineAction

@Entity(
    tableName = "routines",
    indices = [Index("enabled"), Index("createdAt")]
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val triggerPhrase: String = "",
    val actionsJson: String = "[]",
    val enabled: Boolean = true,
    val createdAt: Long,
    val lastRunAt: Long? = null,
    val runCount: Int = 0
)

private val routineGson = Gson()
private val actionsType = object : TypeToken<List<RoutineAction>>() {}.type

fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    name = name,
    triggerPhrase = triggerPhrase,
    actions = routineGson.fromJson(actionsJson, actionsType) ?: emptyList(),
    enabled = enabled,
    createdAt = createdAt,
    lastRunAt = lastRunAt,
    runCount = runCount
)

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    name = name,
    triggerPhrase = triggerPhrase,
    actionsJson = routineGson.toJson(actions),
    enabled = enabled,
    createdAt = createdAt,
    lastRunAt = lastRunAt,
    runCount = runCount
)
