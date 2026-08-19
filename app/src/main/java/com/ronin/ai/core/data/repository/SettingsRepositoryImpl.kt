package com.ronin.ai.core.data.repository

import com.ronin.ai.core.data.datastore.SettingsDataStore
import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.VoiceSettings
import com.ronin.ai.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val defaultAiProvider: Flow<AiProviderType> = dataStore.defaultAiProvider()

    override suspend fun setDefaultAiProvider(type: AiProviderType) {
        dataStore.setDefaultAiProvider(type)
    }

    override fun providerConfig(type: AiProviderType): Flow<AiProviderConfig> =
        dataStore.providerConfig(type)

    override suspend fun getProviderConfig(type: AiProviderType): AiProviderConfig =
        dataStore.getProviderConfig(type)

    override suspend fun saveProviderConfig(config: AiProviderConfig) {
        dataStore.saveProviderConfig(config)
    }

    override suspend fun deleteApiKey(type: AiProviderType) {
        dataStore.deleteApiKey(type)
    }

    override val voiceSettings: Flow<VoiceSettings> = dataStore.voiceSettings()

    override suspend fun getVoiceSettings(): VoiceSettings = dataStore.getVoiceSettings()

    override suspend fun saveVoiceSettings(settings: VoiceSettings) {
        dataStore.saveVoiceSettings(settings)
    }

    override val assistantName: Flow<String> = dataStore.assistantName()

    override suspend fun setAssistantName(name: String) {
        dataStore.setAssistantName(name)
    }

    override val speechOutputEnabled: Flow<Boolean> = dataStore.speechOutputEnabled()

    override suspend fun setSpeechOutputEnabled(enabled: Boolean) {
        dataStore.setSpeechOutputEnabled(enabled)
    }

    override val onboarded: Flow<Boolean> = dataStore.onboarded()

    override suspend fun setOnboarded(value: Boolean) {
        dataStore.setOnboarded(value)
    }
}
