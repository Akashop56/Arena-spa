package com.ronin.ai.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType

@Entity(
    tableName = "memories",
    indices = [Index("type"), Index("createdAt")]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val title: String,
    val content: String,
    val tags: String = "",
    val source: String = "user",
    val createdAt: Long,
    val updatedAt: Long,
    val importance: Int = 1
)

fun MemoryEntity.toDomain(): MemoryItem = MemoryItem(
    id = id,
    type = MemoryType.valueOf(type),
    title = title,
    content = content,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
    importance = importance
)

fun MemoryItem.toEntity(): MemoryEntity = MemoryEntity(
    id = id,
    type = type.name,
    title = title,
    content = content,
    tags = tags.joinToString(","),
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
    importance = importance
)
