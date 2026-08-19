package com.ronin.ai.core.ai.brain

import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts durable user preferences and short-term goals from conversation,
 * e.g. "my name is Arjun", "I prefer short replies", "I want to learn guitar".
 */
@Singleton
class PreferenceExtractor @Inject constructor() {

    private data class Pattern(
        val regex: Regex,
        val type: MemoryType,
        val title: String,
        val importance: Int
    )

    private val patterns = listOf(
        Pattern(Regex("""my name is (.+)""", RegexOption.IGNORE_CASE), MemoryType.PREFERENCE, "User name", 3),
        Pattern(Regex("""call me (.+)""", RegexOption.IGNORE_CASE), MemoryType.PREFERENCE, "User name", 3),
        Pattern(Regex("""i (?:really )?(?:like|love|enjoy) (.+)""", RegexOption.IGNORE_CASE), MemoryType.PREFERENCE, "User likes", 2),
        Pattern(Regex("""i prefer (.+)""", RegexOption.IGNORE_CASE), MemoryType.PREFERENCE, "User preference", 2),
        Pattern(Regex("""i (?:always|never) (.+)""", RegexOption.IGNORE_CASE), MemoryType.PREFERENCE, "User habit", 2),
        Pattern(Regex("""i (?:want|need|would like|am planning) to (.+)""", RegexOption.IGNORE_CASE), MemoryType.SHORT_TERM, "User goal", 1)
    )

    fun extract(input: String): List<MemoryItem> {
        if (input.length > 300) return emptyList()
        val results = mutableListOf<MemoryItem>()
        for (pattern in patterns) {
            val match = pattern.regex.find(input.trim()) ?: continue
            val value = match.groupValues[1].trim()
            if (value.isBlank() || value.length > 120) continue
            results += MemoryItem(
                type = pattern.type,
                title = pattern.title,
                content = value.take(120),
                source = "conversation",
                importance = pattern.importance
            )
        }
        return results.distinctBy { it.title to it.content.lowercase(Locale.ROOT) }
    }
}
