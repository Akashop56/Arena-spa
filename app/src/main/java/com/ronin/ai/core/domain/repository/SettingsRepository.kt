package com.ronin.ai.core.domain.repository

import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.VoiceSettings
import kotlinx.coroutines.flow.Flow

/**
 * Settings repository contract. Provider API keys are stored encrypted
 * (Android Keystore) — implementations must never expose plain keys to disk.
 */
interface SettingsRepository {

    val defaultAiProvider: Flow<AiProviderType>
    suspend fun setDefaultAiProvider(type: AiProviderType)

    fun providerConfig(type: AiProviderType): Flow<AiProviderConfig>
    suspend fun getProviderConfig(type: AiProviderType): AiProviderConfig
    suspend fun saveProviderConfig(config: AiProviderConfig)
    suspend fun deleteApiKey(type: AiProviderType)

    val voiceSettings: Flow<VoiceSettings>
    suspend fun getVoiceSettings(): VoiceSettings
    suspend fun saveVoiceSettings(settings: VoiceSettings)

    val assistantName: Flow<String>
    suspend fun setAssistantName(name: String)

    val speechOutputEnabled: Flow<Boolean>
    suspend fun setSpeechOutputEnabled(enabled: Boolean)

    /** When false, RONIN stops recalling and auto-capturing memories. */
    val memoryEnabled: Flow<Boolean>
    suspend fun setMemoryEnabled(enabled: Boolean)

    val onboarded: Flow<Boolean>
    suspend fun setOnboarded(value: Boolean)
}
