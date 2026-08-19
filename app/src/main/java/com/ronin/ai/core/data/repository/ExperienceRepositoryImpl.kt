package com.ronin.ai.core.data.repository

import com.ronin.ai.core.data.db.dao.ExperienceDao
import com.ronin.ai.core.data.db.entity.ExperienceEntity
import com.ronin.ai.core.data.db.entity.toDomain
import com.ronin.ai.core.data.db.entity.toEntity
import com.ronin.ai.core.domain.model.ExperienceCategory
import com.ronin.ai.core.domain.model.ExperienceItem
import com.ronin.ai.core.domain.repository.ExperienceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperienceRepositoryImpl @Inject constructor(
    private val dao: ExperienceDao
) : ExperienceRepository {

    override fun all(): Flow<List<ExperienceItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun byCategory(category: ExperienceCategory): Flow<List<ExperienceItem>> =
        dao.observeByCategory(category.name).map { list -> list.map { it.toDomain() } }

    override suspend fun recordError(title: String, detail: String, context: String) {
        dao.insert(
            ExperienceEntity(
                category = ExperienceCategory.ERROR.name,
                title = title,
                detail = detail,
                context = context,
                resolved = false,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun recordFix(title: String, detail: String, context: String) {
        dao.insert(
            ExperienceEntity(
                category = ExperienceCategory.FIX.name,
                title = title,
                detail = detail,
                context = context,
                resolved = true,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun recordPreference(title: String, detail: String, context: String) {
        dao.insert(
            ExperienceEntity(
                category = ExperienceCategory.PREFERENCE.name,
                title = title,
                detail = detail,
                context = context,
                resolved = true,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun markResolved(id: Long) {
        dao.markResolved(id)
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    override suspend fun count(): Int = dao.count()
}
