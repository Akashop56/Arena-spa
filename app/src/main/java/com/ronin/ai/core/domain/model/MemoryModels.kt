package com.ronin.ai.core.domain.model

enum class MemoryType(val label: String) {
    SHORT_TERM("Short term"),
    LONG_TERM("Long term"),
    PREFERENCE("Preference"),
    CONVERSATION("Conversation"),
    LEARNED_SOLUTION("Learned solution")
}

data class MemoryItem(
    val id: Long = 0L,
    val type: MemoryType,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val source: String = "user",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val importance: Int = 1
)
