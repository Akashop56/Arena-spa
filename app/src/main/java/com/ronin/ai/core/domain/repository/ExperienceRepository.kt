package com.ronin.ai.core.domain.repository

import com.ronin.ai.core.domain.model.ExperienceCategory
import com.ronin.ai.core.domain.model.ExperienceItem
import kotlinx.coroutines.flow.Flow

interface ExperienceRepository {
    fun all(): Flow<List<ExperienceItem>>
    fun byCategory(category: ExperienceCategory): Flow<List<ExperienceItem>>
    suspend fun recordError(title: String, detail: String, context: String = "")
    suspend fun recordFix(title: String, detail: String, context: String = "")
    suspend fun recordPreference(title: String, detail: String, context: String = "")
    suspend fun markResolved(id: Long)
    suspend fun delete(id: Long)
    suspend fun clearAll()
    suspend fun count(): Int
}
