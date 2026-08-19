package com.ronin.ai.core.domain.model

enum class VoiceProviderType(val displayName: String, val needsKey: Boolean) {
    SYSTEM("System (offline TTS / STT)", false),
    ELEVENLABS("ElevenLabs", true),
    GOOGLE_CLOUD("Google Cloud TTS", true),
    AZURE("Azure Speech", true),
    CUSTOM("Custom voice API (OpenAI-compatible)", true)
}

enum class VoiceLanguage(val code: String, val label: String) {
    EN_US("en-US", "English"),
    HI_IN("hi-IN", "Hindi")
}

/**
 * Everything the voice layer needs to synthesise + recognise speech.
 * Field usage per provider:
 *  - SYSTEM: nothing
 *  - ELEVENLABS: apiKey, voiceId (voice), model (model id)
 *  - GOOGLE_CLOUD: apiKey, voiceId (voice name)
 *  - AZURE: apiKey, voiceId (voice name), model (region, e.g. "eastus")
 *  - CUSTOM: apiKey, voiceId, model (TTS model), endpoint (base URL)
 */
data class VoiceSettings(
    val provider: VoiceProviderType = VoiceProviderType.SYSTEM,
    val apiKey: String = "",
    val voiceId: String = "",
    val language: String = VoiceLanguage.EN_US.code,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val model: String = "",
    val endpoint: String = ""
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
}

/** One-shot result of a speech recognition session. */
data class VoiceRecognitionResult(
    val text: String,
    val language: String,
    val isPartial: Boolean
)
