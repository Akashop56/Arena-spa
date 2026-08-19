package com.ronin.ai.core.domain.usecase

import com.ronin.ai.core.ai.tools.ToolRegistry
import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.domain.model.DashboardData
import com.ronin.ai.core.domain.repository.ConversationRepository
import com.ronin.ai.core.domain.repository.ExperienceRepository
import com.ronin.ai.core.domain.repository.MemoryRepository
import com.ronin.ai.core.domain.repository.RoutineRepository
import com.ronin.ai.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Aggregates everything the dashboard shows in one snapshot. */
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

    private val _data = MutableStateFlow(DashboardData(assistantName = "RONIN"))
    val data: StateFlow<DashboardData> = _data.asStateFlow()

    suspend fun refresh() {
        val name = settingsRepository.assistantName.first()
        val defaultType = settingsRepository.defaultAiProvider.first()
        val config = runCatching {
            settingsRepository.getProviderConfig(defaultType)
        }.getOrNull()

        _data.value = DashboardData(
            assistantName = name,
            defaultProvider = config,
            memoryCount = memoryRepository.count(),
            routineCount = routineRepository.count(),
            skillCount = toolRegistry.definitions().size,
            experienceCount = experienceRepository.count(),
            battery = runCatching { deviceManager.getBatteryState() }.getOrNull(),
            recentMessages = runCatching { conversationRepository.getRecent(3) }.getOrDefault(emptyList())
        )
    }
}
