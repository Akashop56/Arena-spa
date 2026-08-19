package com.ronin.ai.core.ai.brain

import com.ronin.ai.core.ai.providers.AiProviderFactory
import com.ronin.ai.core.ai.providers.toUserMessage
import com.ronin.ai.core.ai.tools.ToolRegistry
import com.ronin.ai.core.domain.model.AiRequestException
import com.ronin.ai.core.domain.model.ChatReply
import com.ronin.ai.core.domain.model.ChatRole
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.PipelineStage
import com.ronin.ai.core.domain.model.ProviderMessage
import com.ronin.ai.core.domain.model.ToolResult
import com.ronin.ai.core.domain.repository.ConversationRepository
import com.ronin.ai.core.domain.repository.ExperienceRepository
import com.ronin.ai.core.domain.repository.MemoryRepository
import com.ronin.ai.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RONIN's reasoning foundation.
 *
 * Pipeline: Input → Understanding → Intent → Planning → Tool Selection →
 * Execution → Evaluation → Memory Update.
 *
 * Only high-level stage labels are exposed ([stage]); the internal chain of
 * thought is never surfaced to the user or the model context.
 */
@Singleton
class AiEngine @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val providerFactory: AiProviderFactory,
    private val intentClassifier: IntentClassifier,
    private val toolRegistry: ToolRegistry,
    private val contextAssembler: ContextAssembler,
    private val preferenceExtractor: PreferenceExtractor,
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val experienceRepository: ExperienceRepository
) {

    private val _stage = MutableStateFlow<PipelineStage?>(null)
    val stage: StateFlow<PipelineStage?> = _stage.asStateFlow()

    suspend fun process(userInput: String): ChatReply {
        val stages = mutableListOf<PipelineStage>()
        fun mark(s: PipelineStage) {
            _stage.value = s
            stages += s
        }

        try {
            // ---- 1. Understanding ----
            mark(PipelineStage.UNDERSTANDING)
            val match = intentClassifier.classify(userInput)
            mark(PipelineStage.INTENT)

            // ---- 2. Planning + tool selection + execution ----
            var toolResult: ToolResult? = null
            var toolName: String? = null
            if (match.type != IntentType.GENERAL) {
                mark(PipelineStage.PLANNING)
                mark(PipelineStage.TOOL_SELECTION)
                val tool = toolRegistry.findFor(match)
                if (tool != null) {
                    mark(PipelineStage.EXECUTION)
                    toolName = tool.definition.name
                    toolResult = runCatching {
                        tool.execute(match.type, match.param, userInput)
                    }.getOrElse { e ->
                        ToolResult(false, e.message ?: "Tool failed", match.type)
                    }
                    if (toolResult.success) {
                        experienceRepository.recordFix(
                            "Tool ${tool.definition.name} succeeded",
                            toolResult.message,
                            userInput.take(200)
                        )
                    } else {
                        experienceRepository.recordError(
                            "Tool ${tool.definition.name} failed",
                            toolResult.message,
                            userInput.take(200)
                        )
                    }
                }
            }

            // ---- 3. Execution via the configured AI provider ----
            mark(PipelineStage.PLANNING)
            val defaultType = settingsRepository.defaultAiProvider.first()
            val config = settingsRepository.getProviderConfig(defaultType)
            val providerReady = config.enabled && config.hasKey

            var reply: String
            var providerUsed: String? = null
            if (!providerReady) {
                reply = when {
                    toolResult != null && toolResult.success -> toolResult.message
                    toolResult != null -> toolResult.message +
                        "\n\n💡 Tip: connect an AI provider in Settings → AI Providers so I can chat with you."
                    else -> "I'm ready, but no AI provider is connected yet. " +
                        "Open Settings → AI Providers, add an API key (Gemini, Groq or OpenAI), and we can talk."
                }
            } else {
                mark(PipelineStage.EXECUTION)
                val provider = providerFactory.forType(defaultType)
                val context = contextAssembler.assemble(userInput)
                val userTurn = if (toolResult != null) {
                    "$userInput\n\n[Tool: $toolName → ${if (toolResult.success) "OK" else "FAILED"}] ${toolResult.message}"
                } else {
                    userInput
                }
                val history = context.recentMessages + ProviderMessage("user", userTurn)
                val messages = if (context.systemPrompt.isNotBlank()) {
                    listOf(ProviderMessage("system", context.systemPrompt)) + history
                } else {
                    history
                }
                try {
                    val response = provider.complete(config, messages, config.temperature)
                    reply = response.text.ifBlank { "I received an empty response from the provider." }
                    providerUsed = defaultType.displayName
                } catch (t: Throwable) {
                    val message = (t as? AiRequestException)?.userMessage
                        ?: t.toUserMessage("The AI provider failed").userMessage
                    experienceRepository.recordError(
                        "AI provider ${defaultType.displayName} failed",
                        message,
                        userInput.take(200)
                    )
                    reply = if (toolResult != null && toolResult.success) {
                        "${toolResult.message}\n\n(Note: the chat model was unavailable — $message)"
                    } else {
                        "⚠️ $message\n\nYou can fix this in Settings → AI Providers and try again."
                    }
                }
            }

            // ---- 4. Evaluation + memory update ----
            mark(PipelineStage.EVALUATION)
            mark(PipelineStage.MEMORY_UPDATE)

            preferenceExtractor.extract(userInput).forEach { pref ->
                val exists = memoryRepository.byType(pref.type).first()
                    .any { it.title == pref.title && it.content == pref.content }
                if (!exists) memoryRepository.save(pref)
            }

            conversationRepository.addMessage(ChatRole.ASSISTANT, reply, toolUsed = toolName)

            return ChatReply(
                reply = reply,
                toolUsed = toolName,
                provider = providerUsed,
                stages = stages
            )
        } catch (t: Throwable) {
            runCatching {
                experienceRepository.recordError(
                    "Pipeline failure",
                    t.message ?: t.javaClass.simpleName,
                    t.stackTraceToString().take(300)
                )
            }
            return ChatReply(
                reply = "Something went wrong on my side — please try again.",
                stages = stages
            )
        } finally {
            _stage.value = null
        }
    }
}
