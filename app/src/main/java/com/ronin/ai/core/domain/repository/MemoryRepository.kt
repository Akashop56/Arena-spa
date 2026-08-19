package com.ronin.ai.core.domain.repository

import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun all(): Flow<List<MemoryItem>>
    fun byType(type: MemoryType): Flow<List<MemoryItem>>
    fun search(query: String): Flow<List<MemoryItem>>
    fun searchAllTypes(query: String, limit: Int): Flow<List<MemoryItem>>

    suspend fun save(item: MemoryItem): Long
    suspend fun delete(id: Long)
    suspend fun deleteByType(type: MemoryType)
    suspend fun clearAll()
    suspend fun count(): Int
    suspend fun countByType(type: MemoryType): Int

    /** Recall memories relevant to a user input (used by the context assembler). */
    suspend fun recallRelevant(input: String, limit: Int): List<MemoryItem>
}
