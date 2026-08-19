package com.ronin.ai.core.ai.brain

import com.ronin.ai.core.domain.model.ChatRole
import com.ronin.ai.core.domain.model.MemoryType
import com.ronin.ai.core.domain.model.ProviderMessage
import com.ronin.ai.core.domain.repository.ConversationRepository
import com.ronin.ai.core.domain.repository.ExperienceRepository
import com.ronin.ai.core.domain.repository.MemoryRepository
import com.ronin.ai.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the LLM needs for one turn, assembled from local knowledge. */
data class AssistantContext(
    val systemPrompt: String,
    val recentMessages: List<ProviderMessage>
)

/**
 * Gathers short-term context (recent conversation), long-term knowledge
 * (relevant memories) and learned lessons into a prompt + message list.
 */
@Singleton
class ContextAssembler @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val experienceRepository: ExperienceRepository,
    private val promptBuilder: PromptBuilder
) {

    suspend fun assemble(userInput: String): AssistantContext {
        val assistantName = settingsRepository.assistantName.first()

        val recent = conversationRepository.getRecent(24).toMutableList()
        // The current user message was stored before the pipeline ran — drop it
        // so it isn't sent twice.
        if (recent.lastOrNull()?.role == ChatRole.USER) {
            recent.removeAt(recent.size - 1)
        }

        val preferences = memoryRepository.byType(MemoryType.PREFERENCE).first().take(8)
        val relevant = memoryRepository.recallRelevant(userInput, 6)
        val learned = experienceRepository.all().first()
            .filter { it.resolved }
            .take(5)

        val systemPrompt = promptBuilder.buildSystemPrompt(
            assistantName = assistantName,
            preferences = preferences,
            relevantMemories = relevant,
            learnedSolutions = learned
        )

        val messages = recent.map { message ->
            ProviderMessage(
                role = if (message.role == ChatRole.USER) "user" else "assistant",
                content = message.content
            )
        }
        return AssistantContext(systemPrompt = systemPrompt, recentMessages = messages)
    }
}
