package com.ronin.ai.core.ai.providers

import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.AiRequestException
import com.ronin.ai.core.domain.model.ProviderMessage
import com.ronin.ai.core.domain.model.ProviderResponse
import com.ronin.ai.core.domain.model.ProviderTestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
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

    /**
     * Streams the reply token-by-token. Providers that cannot stream fall back
     * to emitting the full [complete] result as a single chunk, so callers can
     * always use this API safely.
     */
    fun stream(
        config: AiProviderConfig,
        messages: List<ProviderMessage>,
        temperature: Float
    ): Flow<String> = flow {
        emit(complete(config, messages, temperature).text)
    }

    suspend fun testConnection(config: AiProviderConfig): ProviderTestResult
}

/**
 * Retries [block] on transient failures (timeouts, network drops, 429 and 5xx)
 * using exponential backoff. Authentication and request errors are never
 * retried — they cannot succeed on a second attempt.
 */
internal suspend fun <T> withRetry(
    attempts: Int = 3,
    initialDelayMs: Long = 600L,
    block: suspend () -> T
): T {
    var delayMs = initialDelayMs
    var last: Throwable? = null
    repeat(attempts) { attempt ->
        try {
            return block()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            last = t
            if (!t.isRetryable() || attempt == attempts - 1) throw t
            delay(delayMs)
            delayMs *= 2
        }
    }
    throw last ?: IllegalStateException("Retry failed")
}

/** Only transport hiccups and provider-side overload are worth retrying. */
internal fun Throwable.isRetryable(): Boolean = when (this) {
    is SocketTimeoutException -> true
    is UnknownHostException -> false
    is HttpException -> code() == 429 || code() in 500..599
    is AiRequestException -> httpCode == 429 || (httpCode != null && httpCode in 500..599)
    is IOException -> true
    else -> false
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
