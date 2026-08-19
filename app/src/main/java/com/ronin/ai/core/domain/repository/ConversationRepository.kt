package com.ronin.ai.core.domain.repository

import com.ronin.ai.core.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun messages(): Flow<List<ChatMessage>>
    suspend fun addMessage(role: com.ronin.ai.core.domain.model.ChatRole, content: String, toolUsed: String? = null)
    suspend fun clearAll()
    suspend fun getRecent(limit: Int): List<ChatMessage>
    suspend fun trimTo(maxMessages: Int)
}
