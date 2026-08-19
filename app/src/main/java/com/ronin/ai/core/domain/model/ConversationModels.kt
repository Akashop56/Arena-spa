package com.ronin.ai.core.domain.model

enum class ChatRole { USER, ASSISTANT }

/** UI-facing chat message. */
data class ChatMessage(
    val id: Long = 0L,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolUsed: String? = null
)
