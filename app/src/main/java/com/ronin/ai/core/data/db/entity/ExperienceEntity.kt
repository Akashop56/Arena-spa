package com.ronin.ai.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ronin.ai.core.domain.model.ExperienceCategory
import com.ronin.ai.core.domain.model.ExperienceItem

@Entity(
    tableName = "experiences",
    indices = [Index("category"), Index("createdAt")]
)
data class ExperienceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String,
    val title: String,
    val detail: String,
    val context: String = "",
    val resolved: Boolean = false,
    val createdAt: Long
)

fun ExperienceEntity.toDomain(): ExperienceItem = ExperienceItem(
    id = id,
    category = ExperienceCategory.valueOf(category),
    title = title,
    detail = detail,
    context = context,
    resolved = resolved,
    createdAt = createdAt
)

fun ExperienceItem.toEntity(): ExperienceEntity = ExperienceEntity(
    id = id,
    category = category.name,
    title = title,
    detail = detail,
    context = context,
    resolved = resolved,
    createdAt = createdAt
)
