package com.ronin.ai.core.domain.usecase

import com.ronin.ai.core.ai.providers.AiProviderFactory
import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.ProviderTestResult
import com.ronin.ai.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Connection testing + provider configuration helpers. */
@Singleton
class ProviderUseCases @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val providerFactory: AiProviderFactory
) {

    suspend fun testConnection(type: AiProviderType): ProviderTestResult {
        val config = settingsRepository.getProviderConfig(type)
        return providerFactory.forType(type).testConnection(config)
    }

    suspend fun getConfig(type: AiProviderType): AiProviderConfig =
        settingsRepository.getProviderConfig(type)

    /** @throws IllegalArgumentException when the endpoint/model is invalid. */
    suspend fun saveConfig(config: AiProviderConfig) {
        val normalized = config.withDefaults()
        normalized.validate()?.let { throw IllegalArgumentException(it) }
        settingsRepository.saveProviderConfig(normalized)
    }

    suspend fun deleteKey(type: AiProviderType) {
        settingsRepository.deleteApiKey(type)
    }

    suspend fun setDefault(type: AiProviderType) {
        settingsRepository.setDefaultAiProvider(type)
    }
}
