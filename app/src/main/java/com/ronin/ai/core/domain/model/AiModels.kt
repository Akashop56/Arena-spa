package com.ronin.ai.core.domain.model

/**
 * AI provider catalog. [defaultBaseUrl] is used by the OpenAI-compatible
 * client; CUSTOM providers bring their own endpoint.
 */
enum class AiProviderType(
    val displayName: String,
    val defaultModel: String,
    val defaultBaseUrl: String,
    val description: String
) {
    GEMINI(
        "Gemini",
        "gemini-2.0-flash",
        "",
        "Google Generative Language API"
    ),
    GROQ(
        "Groq",
        "llama-3.3-70b-versatile",
        "https://api.groq.com/openai/v1",
        "Ultra-fast Llama / Mixtral inference"
    ),
    OPENAI(
        "OpenAI",
        "gpt-4o-mini",
        "https://api.openai.com/v1",
        "GPT-4o and GPT-4 family"
    ),
    CUSTOM(
        "Custom (OpenAI-compatible)",
        "",
        "",
        "Any OpenAI-compatible endpoint"
    );

    fun suggestedModels(): List<String> = when (this) {
        GEMINI -> listOf(
            "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-1.5-flash",
            "gemini-1.5-pro", "gemini-2.5-flash"
        )
        GROQ -> listOf(
            "llama-3.3-70b-versatile", "llama-3.1-8b-instant",
            "mixtral-8x7b-32768", "gemma2-9b-it"
        )
        OPENAI -> listOf(
            "gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo"
        )
        CUSTOM -> listOf("gpt-4o-mini", "llama-3.3-70b-versatile")
    }
}

/**
 * Runtime configuration for one AI provider. The API key is decrypted at
 * read time by the settings repository; it never touches disk in plain text.
 */
data class AiProviderConfig(
    val type: AiProviderType,
    val enabled: Boolean = true,
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = "",
    val temperature: Float = 0.7f
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
    val effectiveModel: String
        get() = model.ifBlank { type.defaultModel }
    val effectiveBaseUrl: String
        get() = baseUrl.ifBlank { type.defaultBaseUrl }

    fun withDefaults(): AiProviderConfig = copy(
        model = model.ifBlank { type.defaultModel },
        baseUrl = baseUrl.ifBlank { type.defaultBaseUrl }
    )
}

/** One chat message sent to / received from a model. */
data class ProviderMessage(val role: String, val content: String)

data class ProviderResponse(
    val text: String,
    val model: String?,
    val usageTokens: Int?
)

data class ProviderTestResult(
    val success: Boolean,
    val message: String,
    val latencyMs: Long = 0L,
    val model: String? = null
)

data class AiRequestException(val userMessage: String, val httpCode: Int? = null) :
    Exception(userMessage)
