package com.ronin.ai.core.ai.tools

import com.ronin.ai.core.common.firstWords
import com.ronin.ai.core.domain.model.IntentType
import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType
import com.ronin.ai.core.domain.model.ToolCategory
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.model.ToolResult
import com.ronin.ai.core.domain.repository.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Saves facts (“remember that …”) and recalls stored knowledge. */
@Singleton
class MemoryTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : RoninTool {

    override val definition = ToolDefinition(
        id = "memory",
        name = "Memory",
        description = "Save facts with “remember that …” and recall them with “what do you remember about …”.",
        category = ToolCategory.MEMORY
    )

    override fun matches(intent: IntentType, param: String): Boolean =
        intent == IntentType.SAVE_MEMORY || intent == IntentType.RECALL_MEMORY

    override suspend fun execute(intent: IntentType, param: String, input: String): ToolResult =
        when (intent) {
            IntentType.SAVE_MEMORY -> save(param)
            IntentType.RECALL_MEMORY -> recall(param)
            else -> ToolResult(false, "Unsupported memory operation", intent)
        }

    private suspend fun save(content: String): ToolResult {
        val text = content.trim()
        if (text.isBlank()) {
            return ToolResult(false, "What should I remember?", IntentType.SAVE_MEMORY)
        }
        val item = MemoryItem(
            type = MemoryType.LONG_TERM,
            title = text.firstWords(8),
            content = text,
            source = "conversation",
            importance = 2
        )
        memoryRepository.save(item)
        return ToolResult(
            true,
            "Got it — I'll remember: “${text.firstWords(14)}”.",
            IntentType.SAVE_MEMORY
        )
    }

    private suspend fun recall(query: String): ToolResult {
        val term = query.trim().ifBlank { "notes" }
        val results = memoryRepository.recallRelevant(term, 5)
        if (results.isEmpty()) {
            return ToolResult(
                false,
                "I don't have anything stored about “$term” yet.",
                IntentType.RECALL_MEMORY
            )
        }
        val formatted = results.joinToString("\n\n") { item ->
            "• ${item.title}${if (item.type == MemoryType.PREFERENCE) " (preference)" else ""}\n${item.content}"
        }
        return ToolResult(true, formatted, IntentType.RECALL_MEMORY)
    }
}
