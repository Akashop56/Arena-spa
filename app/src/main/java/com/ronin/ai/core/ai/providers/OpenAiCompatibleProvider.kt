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
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

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
            val response = api.chatCompletions(url, "Bearer ${config.apiKey}", body)
            parseResponse(response, config.effectiveModel)
        } catch (t: AiRequestException) {
            throw t
        } catch (t: Throwable) {
            throw t.toUserMessage("The AI provider could not be reached")
        }
    }

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
