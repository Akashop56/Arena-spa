package com.ronin.ai.core.ai.providers

import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.AiRequestException
import com.ronin.ai.core.domain.model.ProviderMessage
import com.ronin.ai.core.domain.model.ProviderResponse
import com.ronin.ai.core.domain.model.ProviderTestResult
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Contract implemented by every AI brain provider. */
interface AiProvider {
    val type: AiProviderType

    suspend fun complete(
        config: AiProviderConfig,
        messages: List<ProviderMessage>,
        temperature: Float
    ): ProviderResponse

    suspend fun testConnection(config: AiProviderConfig): ProviderTestResult
}

/** Maps transport + HTTP errors to human-readable messages. */
internal fun Throwable.toUserMessage(fallback: String): AiRequestException {
    val (message, code) = when (this) {
        is HttpException -> when (code()) {
            401 -> "Authentication failed (401) — check the API key" to 401
            403 -> "Access denied (403) — check key permissions" to 403
            404 -> "Model or endpoint not found (404) — check the model name" to 404
            429 -> "Rate limit reached (429) — wait a moment and retry" to 429
            in 500..599 -> "Provider server error (${code()}) — try again shortly" to code()
            else -> "Provider error (${code()})" to code()
        }
        is SocketTimeoutException -> "Request timed out — check your connection" to null
        is UnknownHostException -> "Network unreachable — check your connection" to null
        else -> fallback to null
    }
    return AiRequestException(message, code)
}

/** Builds the OpenAI-compatible request body shared by OpenAI / Groq / custom. */
internal fun openAiRequestBody(
    model: String,
    messages: List<ProviderMessage>,
    temperature: Float,
    maxTokens: Int
): com.google.gson.JsonObject = com.google.gson.JsonObject().apply {
    addProperty("model", model)
    add("messages", com.google.gson.JsonArray().apply {
        messages.forEach { m ->
            add(com.google.gson.JsonObject().apply {
                addProperty("role", m.role)
                addProperty("content", m.content)
            })
        }
    })
    addProperty("temperature", temperature)
    addProperty("max_tokens", maxTokens)
}
