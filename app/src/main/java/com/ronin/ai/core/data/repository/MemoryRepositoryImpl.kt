package com.ronin.ai.core.data.repository

import com.ronin.ai.core.ai.brain.MemoryRanker
import com.ronin.ai.core.data.db.dao.MemoryDao
import com.ronin.ai.core.data.db.entity.MemoryEntity
import com.ronin.ai.core.data.db.entity.toDomain
import com.ronin.ai.core.data.db.entity.toEntity
import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType
import com.ronin.ai.core.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val dao: MemoryDao,
    private val ranker: MemoryRanker
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

    /**
     * Recall used by the reasoning pipeline.
     *
     * Previously this took the first three tokens of the raw input and OR-ed
     * them into LIKE queries; because questions start with filler words
     * ("what do you know about my coffee preference"), the real subject was
     * usually discarded and nothing was recalled. Now the query is reduced to
     * content-bearing terms, each is used to gather candidates, and
     * [MemoryRanker] scores them on lexical overlap x importance x recency.
     */
    override suspend fun recallRelevant(input: String, limit: Int): List<MemoryItem> {
        val terms = ranker.terms(input)
        if (terms.isEmpty()) return emptyList()

        val candidates = LinkedHashMap<Long, MemoryItem>()
        for (term in terms) {
            for (entity in dao.searchNonConversation(term, CANDIDATES_PER_TERM)) {
                val item = entity.toDomain()
                candidates[item.id] = item
            }
            if (candidates.size >= MAX_CANDIDATES) break
        }
        return ranker.rank(input, candidates.values.toList(), limit)
    }

    private companion object {
        const val CANDIDATES_PER_TERM = 8
        const val MAX_CANDIDATES = 40
    }
}
