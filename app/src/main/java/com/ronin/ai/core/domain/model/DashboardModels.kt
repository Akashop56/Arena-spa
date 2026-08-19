package com.ronin.ai.core.domain.model

data class DashboardData(
    val assistantName: String,
    val defaultProvider: AiProviderConfig? = null,
    val memoryCount: Int = 0,
    val routineCount: Int = 0,
    val skillCount: Int = 0,
    val experienceCount: Int = 0,
    val battery: BatteryState? = null,
    val recentMessages: List<ChatMessage> = emptyList()
)
