package com.ronin.ai.core.domain.usecase

import com.ronin.ai.core.ai.tools.ToolRegistry
import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.domain.model.DashboardData
import com.ronin.ai.core.domain.repository.ConversationRepository
import com.ronin.ai.core.domain.repository.ExperienceRepository
import com.ronin.ai.core.domain.repository.MemoryRepository
import com.ronin.ai.core.domain.repository.RoutineRepository
import com.ronin.ai.core.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates everything the dashboard shows.
 *
 * This is now a cold, reactive stream: memory counts, routines and the recent
 * conversation update themselves as the underlying tables change. Previously
 * the screen took a one-shot snapshot on init, so the numbers went stale as
 * soon as the user chatted or saved a memory.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DashboardUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val memoryRepository: MemoryRepository,
    private val routineRepository: RoutineRepository,
    private val experienceRepository: ExperienceRepository,
    private val conversationRepository: ConversationRepository,
    private val deviceManager: DeviceManager,
    private val toolRegistry: ToolRegistry
) {

    fun observe(): Flow<DashboardData> = combine(
        settingsRepository.assistantName,
        settingsRepository.defaultAiProvider,
        memoryRepository.all(),
        routineRepository.routines(),
        conversationRepository.messages()
    ) { name, defaultType, memories, routines, messages ->
        DashboardSnapshot(name, defaultType, memories.size, routines.size, messages)
    }.flatMapLatest { snapshot ->
        experienceRepository.all().map { experiences ->
            val config = runCatching {
                settingsRepository.getProviderConfig(snapshot.defaultType)
            }.getOrNull()
            DashboardData(
                assistantName = snapshot.name,
                defaultProvider = config,
                memoryCount = snapshot.memoryCount,
                routineCount = snapshot.routineCount,
                skillCount = toolRegistry.definitions().size,
                experienceCount = experiences.size,
                battery = runCatching { deviceManager.getBatteryState() }.getOrNull(),
                recentMessages = snapshot.messages.takeLast(3)
            )
        }
    }

    private data class DashboardSnapshot(
        val name: String,
        val defaultType: com.ronin.ai.core.domain.model.AiProviderType,
        val memoryCount: Int,
        val routineCount: Int,
        val messages: List<com.ronin.ai.core.domain.model.ChatMessage>
    )

    /** One-shot snapshot, used for pull-to-refresh style actions. */
    suspend fun snapshot(): DashboardData = observe().first()
}
