package com.ronin.ai.core.ai.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.ronin.ai.core.common.Constants
import com.ronin.ai.core.domain.model.AiProviderConfig
import com.ronin.ai.core.domain.model.AiProviderType
import com.ronin.ai.core.domain.model.AiRequestException
import com.ronin.ai.core.domain.model.ProviderMessage
import com.ronin.ai.core.domain.model.ProviderResponse
import com.ronin.ai.core.domain.model.ProviderTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * OpenAI-compatible chat completions client. Used for OpenAI, Groq and any
 * custom endpoint exposing POST /chat/completions.
 */
class OpenAiCompatibleProvider(
    private val api: AiApi,
    private val gson: Gson,
    override val type: AiProviderType
) : AiProvider {

    override suspend fun complete(
        config: AiProviderConfig,
        messages: List<ProviderMessage>,
        temperature: Float
    ): ProviderResponse = withContext(Dispatchers.IO) {
        val base = config.effectiveBaseUrl
        if (base.isBlank()) {
            throw AiRequestException("Base URL is required for this provider")
        }
        if (config.apiKey.isBlank()) {
            throw AiRequestException("No API key configured — add one in Settings → AI Providers")
        }
        val url = base.trimEnd('/') + "/chat/completions"
        val body = openAiRequestBody(
            model = config.effectiveModel,
            messages = messages,
            temperature = temperature,
            maxTokens = Constants.MAX_OUTPUT_TOKENS
        )
        try {
            withRetry {
                val response = api.chatCompletions(url, "Bearer ${config.apiKey}", body)
                parseResponse(response, config.effectiveModel)
            }
        } catch (t: AiRequestException) {
            throw t
        } catch (t: Throwable) {
            throw t.toUserMessage("The AI provider could not be reached")
        }
    }

    /**
     * Streams an OpenAI-compatible SSE response. Each `data:` line carries a
     * delta; `[DONE]` terminates the stream. If the endpoint rejects streaming
     * we transparently fall back to a single buffered completion.
     */
    override fun stream(
        config: AiProviderConfig,
        messages: List<ProviderMessage>,
        temperature: Float
    ): Flow<String> = flow {
        val base = config.effectiveBaseUrl
        if (base.isBlank()) throw AiRequestException("Base URL is required for this provider")
        if (config.apiKey.isBlank()) {
            throw AiRequestException("No API key configured — add one in Settings → AI Providers")
        }
        val url = base.trimEnd('/') + "/chat/completions"
        val body = openAiRequestBody(
            model = config.effectiveModel,
            messages = messages,
            temperature = temperature,
            maxTokens = Constants.MAX_OUTPUT_TOKENS
        ).apply { addProperty("stream", true) }

        val responseBody = try {
            api.chatCompletionsStream(url, "Bearer ${config.apiKey}", body)
        } catch (t: Throwable) {
            // Endpoint does not support streaming (or transient failure):
            // fall back to the buffered call so the user still gets a reply.
            emit(complete(config, messages, temperature).text)
            return@flow
        }

        var emittedAnything = false
        responseBody.use { rb ->
            val source = rb.source()
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty()) continue
                if (payload == "[DONE]") break
                val delta = runCatching {
                    gson.fromJson(payload, JsonObject::class.java)
                        ?.getAsJsonArray("choices")
                        ?.takeIf { it.size() > 0 }
                        ?.get(0)?.asJsonObject
                        ?.getAsJsonObject("delta")
                        ?.get("content")
                        ?.takeIf { !it.isJsonNull }
                        ?.asString
                }.getOrNull()
                if (!delta.isNullOrEmpty()) {
                    emittedAnything = true
                    emit(delta)
                }
            }
        }
        if (!emittedAnything) {
            emit(complete(config, messages, temperature).text)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(config: AiProviderConfig): ProviderTestResult =
        withContext(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            var latency = 0L
            try {
                val base = config.effectiveBaseUrl
                if (base.isBlank()) {
                    return@withContext ProviderTestResult(false, "Base URL is required", 0, config.effectiveModel)
                }
                if (config.apiKey.isBlank()) {
                    return@withContext ProviderTestResult(false, "No API key configured", 0, config.effectiveModel)
                }
                val url = base.trimEnd('/') + "/chat/completions"
                val body = openAiRequestBody(
                    model = config.effectiveModel,
                    messages = listOf(
                        ProviderMessage("user", "Reply with exactly: OK")
                    ),
                    temperature = 0f,
                    maxTokens = 8
                )
                val response = api.chatCompletions(url, "Bearer ${config.apiKey}", body)
                latency = System.currentTimeMillis() - started
                val text = parseResponse(response, config.effectiveModel).text
                ProviderTestResult(
                    success = true,
                    message = "Connected · ${text.take(40)}",
                    latencyMs = latency,
                    model = config.effectiveModel
                )
            } catch (t: Throwable) {
                latency = System.currentTimeMillis() - started
                ProviderTestResult(
                    success = false,
                    message = t.toUserMessage("Connection failed").userMessage,
                    latencyMs = latency,
                    model = config.effectiveModel
                )
            }
        }

    private fun parseResponse(json: JsonObject, model: String): ProviderResponse {
        val choices = json.getAsJsonArray("choices")
            ?: throw AiRequestException("Unexpected provider response (missing choices)")
        if (choices.size() == 0) {
            throw AiRequestException("Provider returned an empty response")
        }
        val message = choices[0].asJsonObject.getAsJsonObject("message")
        val content = message?.get("content")?.let { el ->
            if (el.isJsonNull) null else runCatching { el.asString }.getOrNull()
        }.orEmpty()
        val usage = runCatching {
            json.getAsJsonObject("usage")?.get("total_tokens")?.asInt
        }.getOrNull()
        return ProviderResponse(text = content.trim(), model = model, usageTokens = usage)
    }
}
