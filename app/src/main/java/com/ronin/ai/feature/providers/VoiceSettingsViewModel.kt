package com.ronin.ai.feature.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.device.VoiceService
import com.ronin.ai.core.domain.model.VoiceProviderType
import com.ronin.ai.core.domain.model.VoiceSettings
import com.ronin.ai.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val voiceService: VoiceService
) : ViewModel() {

    private val _settings = MutableStateFlow(VoiceSettings())
    val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.voiceSettings.collect { _settings.value = it }
        }
    }

    fun onProviderChange(provider: VoiceProviderType) {
        _settings.value = _settings.value.copy(provider = provider)
    }

    fun onApiKeyChange(value: String) {
        _settings.value = _settings.value.copy(apiKey = value)
    }

    fun onVoiceIdChange(value: String) {
        _settings.value = _settings.value.copy(voiceId = value)
    }

    fun onLanguageChange(code: String) {
        _settings.value = _settings.value.copy(language = code)
    }

    fun onSpeedChange(value: Float) {
        _settings.value = _settings.value.copy(speed = value)
    }

    fun onPitchChange(value: Float) {
        _settings.value = _settings.value.copy(pitch = value)
    }

    fun onModelChange(value: String) {
        _settings.value = _settings.value.copy(model = value)
    }

    fun onEndpointChange(value: String) {
        _settings.value = _settings.value.copy(endpoint = value)
    }

    fun save() {
        viewModelScope.launch {
            runCatching { settingsRepository.saveVoiceSettings(_settings.value) }
                .onSuccess { _saved.value = true }
        }
    }

    fun testVoice() {
        viewModelScope.launch {
            settingsRepository.saveVoiceSettings(_settings.value)
            voiceService.testVoice()
        }
    }
}
