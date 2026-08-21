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

    /**
     * @param onToken receives streamed chunks of the reply as they arrive.
     * Pass null for a buffered (non-streaming) result.
     */
    suspend fun invoke(
        input: String,
        onToken: ((String) -> Unit)? = null
    ): ChatReply {
        val text = input.trim()
        if (text.isEmpty()) return ChatReply(reply = "")
        conversationRepository.addMessage(ChatRole.USER, text)
        return engine.process(text, onToken)
    }
}
