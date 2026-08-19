package com.ronin.ai.core.data.repository

import com.ronin.ai.core.common.Constants
import com.ronin.ai.core.data.db.dao.ConversationDao
import com.ronin.ai.core.data.db.entity.toDomain
import com.ronin.ai.core.data.db.entity.toEntity
import com.ronin.ai.core.domain.model.ChatMessage
import com.ronin.ai.core.domain.model.ChatRole
import com.ronin.ai.core.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val dao: ConversationDao
) : ConversationRepository {

    override fun messages(): Flow<List<ChatMessage>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addMessage(role: ChatRole, content: String, toolUsed: String?) {
        dao.insert(
            com.ronin.ai.core.data.db.entity.ConversationEntity(
                role = role.name,
                content = content,
                timestamp = System.currentTimeMillis(),
                toolUsed = toolUsed
            )
        )
        trimTo(Constants.MAX_CONVERSATION_MESSAGES)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    override suspend fun getRecent(limit: Int): List<ChatMessage> =
        dao.recent(limit).reversed().map { it.toDomain() }

    override suspend fun trimTo(maxMessages: Int) {
        dao.trimTo(maxMessages)
    }
}
