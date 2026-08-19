package com.ronin.ai.core.domain.usecase

import com.ronin.ai.core.ai.brain.AiEngine
import com.ronin.ai.core.domain.model.ChatReply
import com.ronin.ai.core.domain.model.ChatRole
import com.ronin.ai.core.domain.model.PipelineStage
import com.ronin.ai.core.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point for the chat: stores the user message (conversation memory)
 * and runs the full reasoning pipeline.
 */
@Singleton
class SendMessageUseCase @Inject constructor(
    private val engine: AiEngine,
    private val conversationRepository: ConversationRepository
) {

    val stage: StateFlow<PipelineStage?> = engine.stage

    suspend fun invoke(input: String): ChatReply {
        val text = input.trim()
        if (text.isEmpty()) return ChatReply(reply = "")
        conversationRepository.addMessage(ChatRole.USER, text)
        return engine.process(text)
    }
}
