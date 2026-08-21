package com.ronin.ai.feature.providers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.ProviderTestResult
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

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    private val _testResult = MutableStateFlow<ProviderTestResult?>(null)
    val testResult: StateFlow<ProviderTestResult?> = _testResult.asStateFlow()

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
            _error.value = null
            runCatching {
                providerUseCases.saveConfig(_config.value)
            }.onSuccess {
                _saved.value = true
            }.onFailure { e ->
                _error.value = e.message ?: "Couldn't save"
            }
        }
    }

    /**
     * Saves the current values first, then performs a live round-trip so the
     * result reflects exactly what RONIN will use at runtime.
     */
    fun testConnection() {
        if (_testing.value) return
        viewModelScope.launch {
            _error.value = null
            _testResult.value = null
            _testing.value = true
            try {
                providerUseCases.saveConfig(_config.value)
                _saved.value = true
                _testResult.value = providerUseCases.testConnection(type)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Connection test failed"
            } finally {
                _testing.value = false
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
