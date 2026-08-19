package com.ronin.ai.feature.providers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.usecase.ProviderUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiProviderEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val providerUseCases: ProviderUseCases
) : ViewModel() {

    val type: AiProviderType = runCatching {
        AiProviderType.valueOf(savedStateHandle.get<String>("type") ?: AiProviderType.GEMINI.name)
    }.getOrDefault(AiProviderType.GEMINI)

    private val _config = MutableStateFlow(AiProviderConfig(type = type))
    val config: StateFlow<AiProviderConfig> = _config.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            _config.value = runCatching { providerUseCases.getConfig(type) }
                .getOrDefault(AiProviderConfig(type = type, model = type.defaultModel))
        }
    }

    fun onApiKeyChange(value: String) {
        _config.value = _config.value.copy(apiKey = value)
    }

    fun onModelChange(value: String) {
        _config.value = _config.value.copy(model = value)
    }

    fun onBaseUrlChange(value: String) {
        _config.value = _config.value.copy(baseUrl = value)
    }

    fun onTemperatureChange(value: Float) {
        _config.value = _config.value.copy(temperature = value)
    }

    fun onEnabledChange(value: Boolean) {
        _config.value = _config.value.copy(enabled = value)
    }

    fun save() {
        viewModelScope.launch {
            runCatching {
                providerUseCases.saveConfig(_config.value)
            }.onSuccess {
                _saved.value = true
            }.onFailure { e ->
                _error.value = e.message ?: "Couldn't save"
            }
        }
    }

    fun deleteKey() {
        viewModelScope.launch {
            providerUseCases.deleteKey(type)
            _config.value = _config.value.copy(apiKey = "")
        }
    }

    fun dismissError() {
        _error.value = null
    }
}
