package com.ronin.ai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.VoiceProviderType
import com.ronin.ai.core.domain.repository.ExperienceRepository
import com.ronin.ai.core.domain.repository.SettingsRepository
import com.ronin.ai.core.domain.usecase.DeviceUseCases
import com.ronin.ai.core.domain.usecase.MemoryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val memoryUseCases: MemoryUseCases,
    private val experienceRepository: ExperienceRepository,
    private val deviceUseCases: DeviceUseCases
) : ViewModel() {

    val assistantName: StateFlow<String> = settingsRepository.assistantName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "RONIN")

    val speechOutput: StateFlow<Boolean> = settingsRepository.speechOutputEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultProvider: StateFlow<AiProviderType> = settingsRepository.defaultAiProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiProviderType.GEMINI)

    val voiceProvider: StateFlow<VoiceProviderType> = settingsRepository.voiceSettings
        .map { it.provider }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VoiceProviderType.SYSTEM)

    fun setAssistantName(name: String) {
        viewModelScope.launch { settingsRepository.setAssistantName(name) }
    }

    fun setSpeechOutput(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSpeechOutputEnabled(enabled) }
    }

    fun clearAllMemory() {
        viewModelScope.launch { memoryUseCases.clearAll() }
    }

    fun clearExperiences() {
        viewModelScope.launch { experienceRepository.clearAll() }
    }

    fun isNotificationAccessGranted(): Boolean = deviceUseCases.isNotificationAccessGranted()

    fun openNotificationAccessSettings() = deviceUseCases.openNotificationAccessSettings()
}
