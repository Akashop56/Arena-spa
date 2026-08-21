package com.ronin.ai.feature.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.ProviderTestResult
import com.ronin.ai.core.domain.repository.SettingsRepository
import com.ronin.ai.core.domain.usecase.ProviderUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderUiState(
    val config: AiProviderConfig,
    val isDefault: Boolean = false,
    val testing: Boolean = false,
    val testResult: ProviderTestResult? = null
)

@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val providerUseCases: ProviderUseCases
) : ViewModel() {

    private val _providers = MutableStateFlow<List<ProviderUiState>>(emptyList())
    val providers: StateFlow<List<ProviderUiState>> = _providers.asStateFlow()

    private val _testingType = MutableStateFlow<AiProviderType?>(null)
    val testingType: StateFlow<AiProviderType?> = _testingType.asStateFlow()

    private val _testResult = MutableStateFlow<Pair<AiProviderType, ProviderTestResult>?>(null)
    val testResult: StateFlow<Pair<AiProviderType, ProviderTestResult>?> = _testResult.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val default = settingsRepository.defaultAiProvider.first()
            _providers.value = AiProviderType.entries.map { type ->
                val config = runCatching { settingsRepository.getProviderConfig(type) }
                    .getOrDefault(AiProviderConfig(type = type, model = type.defaultModel))
                ProviderUiState(config = config, isDefault = type == default)
            }
        }
    }

    fun setDefault(type: AiProviderType) {
        viewModelScope.launch {
            providerUseCases.setDefault(type)
            refresh()
        }
    }

    fun setEnabled(type: AiProviderType, enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                val config = providerUseCases.getConfig(type)
                providerUseCases.saveConfig(config.copy(enabled = enabled))
            }.onFailure { e -> _error.value = e.message ?: "Couldn't update provider" }
            refresh()
        }
    }

    fun testConnection(type: AiProviderType) {
        if (_busy.value) return
        viewModelScope.launch {
            _testingType.value = type
            _testResult.value = null
            _error.value = null
            _busy.value = true
            try {
                _testResult.value = type to providerUseCases.testConnection(type)
            } catch (t: Throwable) {
                // Without this the spinner stuck on forever when the call threw.
                _error.value = t.message ?: "Connection test failed"
            } finally {
                _testingType.value = null
                _busy.value = false
            }
            refresh()
        }
    }

    fun deleteKey(type: AiProviderType) {
        viewModelScope.launch {
            runCatching { providerUseCases.deleteKey(type) }
                .onFailure { e -> _error.value = e.message ?: "Couldn't delete key" }
            refresh()
        }
    }

    fun dismissError() {
        _error.value = null
    }
}
