package com.ronin.ai.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.ronin.ai.core.data.security.SecureVault
import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.VoiceSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.roninDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ronin_settings"
)

/**
 * All persisted settings. API keys are stored as Keystore-encrypted blobs;
 * everything else is plain preferences.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val vault: SecureVault
) {

    private object Keys {
        val DEFAULT_AI_PROVIDER = stringPreferencesKey("default_ai_provider")
        val AI_CONFIG_GEMINI = stringPreferencesKey("ai_config_gemini")
        val AI_CONFIG_GROQ = stringPreferencesKey("ai_config_groq")
        val AI_CONFIG_OPENAI = stringPreferencesKey("ai_config_openai")
        val AI_CONFIG_CUSTOM = stringPreferencesKey("ai_config_custom")
        val AI_KEY_GEMINI = stringPreferencesKey("ai_key_gemini")
        val AI_KEY_GROQ = stringPreferencesKey("ai_key_groq")
        val AI_KEY_OPENAI = stringPreferencesKey("ai_key_openai")
        val AI_KEY_CUSTOM = stringPreferencesKey("ai_key_custom")
        val VOICE_SETTINGS = stringPreferencesKey("voice_settings")
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val SPEECH_OUTPUT = booleanPreferencesKey("speech_output")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    private fun configKey(type: AiProviderType) = when (type) {
        AiProviderType.GEMINI -> Keys.AI_CONFIG_GEMINI
        AiProviderType.GROQ -> Keys.AI_CONFIG_GROQ
        AiProviderType.OPENAI -> Keys.AI_CONFIG_OPENAI
        AiProviderType.CUSTOM -> Keys.AI_CONFIG_CUSTOM
    }

    private fun keyBlobKey(type: AiProviderType) = when (type) {
        AiProviderType.GEMINI -> Keys.AI_KEY_GEMINI
        AiProviderType.GROQ -> Keys.AI_KEY_GROQ
        AiProviderType.OPENAI -> Keys.AI_KEY_OPENAI
        AiProviderType.CUSTOM -> Keys.AI_KEY_CUSTOM
    }

    fun defaultAiProvider(): Flow<AiProviderType> =
        context.roninDataStore.data.map { prefs ->
            prefs[Keys.DEFAULT_AI_PROVIDER]?.let { runCatching { AiProviderType.valueOf(it) }.getOrNull() }
                ?: AiProviderType.GEMINI
        }

    suspend fun setDefaultAiProvider(type: AiProviderType) {
        context.roninDataStore.edit { it[Keys.DEFAULT_AI_PROVIDER] = type.name }
    }

    fun providerConfig(type: AiProviderType): Flow<AiProviderConfig> =
        context.roninDataStore.data.map { prefs ->
            val json = prefs[configKey(type)] ?: return@map defaultConfig(type)
            val config = runCatching { gson.fromJson(json, AiProviderConfig::class.java) }
                .getOrNull() ?: defaultConfig(type)
            val encryptedKey = prefs[keyBlobKey(type)]
            val apiKey = encryptedKey?.let { vault.decrypt(it) } ?: ""
            config.copy(
                type = type,
                apiKey = apiKey,
                model = config.model.ifBlank { type.defaultModel }
            )
        }

    suspend fun getProviderConfig(type: AiProviderType): AiProviderConfig =
        providerConfig(type).first()

    suspend fun saveProviderConfig(config: AiProviderConfig) {
        val plain = config.copy(apiKey = "")
        val json = gson.toJson(plain)
        context.roninDataStore.edit { prefs ->
            prefs[configKey(config.type)] = json
            if (config.apiKey.isNotBlank()) {
                prefs[keyBlobKey(config.type)] = vault.encrypt(config.apiKey)
            }
        }
    }

    suspend fun deleteApiKey(type: AiProviderType) {
        context.roninDataStore.edit { it.remove(keyBlobKey(type)) }
    }

    fun voiceSettings(): Flow<VoiceSettings> =
        context.roninDataStore.data.map { prefs ->
            val json = prefs[Keys.VOICE_SETTINGS]
                ?: return@map VoiceSettings()
            val settings = runCatching { gson.fromJson(json, VoiceSettings::class.java) }
                .getOrNull() ?: VoiceSettings()
            // The stored apiKey field holds an encrypted blob.
            val decrypted = if (settings.apiKey.isBlank()) "" else (vault.decrypt(settings.apiKey) ?: "")
            settings.copy(apiKey = decrypted)
        }

    suspend fun getVoiceSettings(): VoiceSettings = voiceSettings().first()

    suspend fun saveVoiceSettings(settings: VoiceSettings) {
        // The apiKey field of the JSON is the *encrypted* blob.
        val toStore = if (settings.apiKey.isBlank()) {
            settings
        } else {
            settings.copy(apiKey = vault.encrypt(settings.apiKey))
        }
        context.roninDataStore.edit { it[Keys.VOICE_SETTINGS] = gson.toJson(toStore) }
    }

    fun assistantName(): Flow<String> =
        context.roninDataStore.data.map { it[Keys.ASSISTANT_NAME] ?: "RONIN" }

    suspend fun setAssistantName(name: String) {
        context.roninDataStore.edit { it[Keys.ASSISTANT_NAME] = name.trim().ifBlank { "RONIN" } }
    }

    fun speechOutputEnabled(): Flow<Boolean> =
        context.roninDataStore.data.map { it[Keys.SPEECH_OUTPUT] ?: false }

    suspend fun setSpeechOutputEnabled(enabled: Boolean) {
        context.roninDataStore.edit { it[Keys.SPEECH_OUTPUT] = enabled }
    }

    fun onboarded(): Flow<Boolean> =
        context.roninDataStore.data.map { it[Keys.ONBOARDED] ?: false }

    suspend fun setOnboarded(value: Boolean) {
        context.roninDataStore.edit { it[Keys.ONBOARDED] = value }
    }

    private fun defaultConfig(type: AiProviderType): AiProviderConfig =
        AiProviderConfig(type = type, model = type.defaultModel)
}
