package com.ronin.ai.core.domain.usecase

import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType
import com.ronin.ai.core.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** All memory operations: save, retrieve, search, delete, clear. */
@Singleton
class MemoryUseCases @Inject constructor(
    private val memoryRepository: MemoryRepository
) {

    fun all(): Flow<List<MemoryItem>> = memoryRepository.all()

    fun byType(type: MemoryType): Flow<List<MemoryItem>> = memoryRepository.byType(type)

    fun search(query: String): Flow<List<MemoryItem>> = memoryRepository.search(query)

    suspend fun save(item: MemoryItem): Long = memoryRepository.save(item)

    suspend fun delete(id: Long) = memoryRepository.delete(id)

    suspend fun clearAll() = memoryRepository.clearAll()

    suspend fun deleteByType(type: MemoryType) = memoryRepository.deleteByType(type)
}
