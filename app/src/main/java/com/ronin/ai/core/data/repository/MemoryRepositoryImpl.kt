package com.ronin.ai.core.data.repository

import com.ronin.ai.core.data.db.dao.MemoryDao
import com.ronin.ai.core.data.db.entity.MemoryEntity
import com.ronin.ai.core.data.db.entity.toDomain
import com.ronin.ai.core.data.db.entity.toEntity
import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType
import com.ronin.ai.core.domain.repository.MemoryRepository
import com.ronin.ai.core.common.keywords
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val dao: MemoryDao
) : MemoryRepository {

    override fun all(): Flow<List<MemoryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun byType(type: MemoryType): Flow<List<MemoryItem>> =
        dao.observeByType(type.name).map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<MemoryItem>> =
        dao.observeSearch(query.trim()).map { list -> list.map { it.toDomain() } }

    override fun searchAllTypes(query: String, limit: Int): Flow<List<MemoryItem>> =
        dao.observeSearchNonConversation(query.trim(), limit).map { list -> list.map { it.toDomain() } }

    override suspend fun save(item: MemoryItem): Long =
        dao.insert(item.toEntity())

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteByType(type: MemoryType) {
        dao.deleteByType(type.name)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    override suspend fun count(): Int = dao.count()

    override suspend fun countByType(type: MemoryType): Int = dao.countByType(type.name)

    override suspend fun findSimilar(type: MemoryType, title: String, content: String): Boolean =
        dao.countMatching(type.name, title, content) > 0

    override suspend fun recallRelevant(input: String, limit: Int): List<MemoryItem> {
        val terms = input.keywords().take(3)
        val results = LinkedHashMap<Long, MemoryItem>()
        for (term in terms) {
            val matches = dao.searchNonConversation(term, 6).map { it.toDomain() }
            for (m in matches) results[m.id] = m
            if (results.size >= limit) break
        }
        return results.values
            .sortedWith(compareByDescending<MemoryItem> { it.importance }.thenByDescending { it.updatedAt })
            .take(limit)
    }
}
