package com.ronin.ai.core.ai.providers

import com.google.gson.Gson
import com.google.gson.JsonArray
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
import java.net.URLEncoder
import kotlin.system.measureTimeMillis

/**
 * Google Gemini client (v1beta generateContent REST API).
 * Gemini only accepts "user"/"model" roles, so system instructions travel
 * in the systemInstruction field and consecutive same-role messages are
 * merged (the API rejects duplicated roles).
 */
class GeminiProvider(
    private val api: AiApi,
    private val gson: Gson
) : AiProvider {

    override val type: AiProviderType = AiProviderType.GEMINI

    override suspend fun complete(
        config: AiProviderConfig,
        messages: List<ProviderMessage>,
        temperature: Float
    ): ProviderResponse = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            throw AiRequestException("No API key configured — add one in Settings → AI Providers")
        }
        val model = config.effectiveModel
        val key = URLEncoder.encode(config.apiKey, "UTF-8")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

        var systemInstruction: JsonObject? = null
        val contents = JsonArray()
        var lastRole: String? = null
        var lastContent = StringBuilder()

        fun flush() {
            val role = lastRole ?: return
            contents.add(JsonObject().apply {
                addProperty("role", if (role == "assistant") "model" else "user")
                add("parts", JsonArray().apply {
                    add(JsonObject().apply { addProperty("text", lastContent.toString().trim()) })
                })
            })
        }

        for (m in messages) {
            when (m.role) {
                "system" -> {
                    systemInstruction = JsonObject().apply {
                        add("parts", JsonArray().apply {
                            add(JsonObject().apply { addProperty("text", m.content) })
                        })
                    }
                }
                else -> {
                    val normalized = if (m.role == "assistant") "assistant" else "user"
                    if (normalized == lastRole) {
                        lastContent.append("\n").append(m.content)
                    } else {
                        flush()
                        lastRole = normalized
                        lastContent = StringBuilder(m.content)
                    }
                }
            }
        }
        flush()

        val body = JsonObject().apply {
            add("contents", contents)
            systemInstruction?.let { add("systemInstruction", it) }
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", temperature.toDouble())
                addProperty("maxOutputTokens", Constants.MAX_OUTPUT_TOKENS)
                addProperty("candidateCount", 1)
            })
        }

        try {
            val response = api.generateContent(url, body)
            parseResponse(response, model)
        } catch (t: AiRequestException) {
            throw t
        } catch (t: Throwable) {
            throw t.toUserMessage("Gemini could not be reached")
        }
    }

    override suspend fun testConnection(config: AiProviderConfig): ProviderTestResult =
        withContext(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            try {
                if (config.apiKey.isBlank()) {
                    return@withContext ProviderTestResult(false, "No API key configured", 0, config.effectiveModel)
                }
                val testConfig = config.copy(model = config.effectiveModel.ifBlank { "gemini-2.0-flash" })
                val response = complete(
                    testConfig,
                    listOf(ProviderMessage("user", "Reply with exactly: OK")),
                    temperature = 0f
                )
                ProviderTestResult(
                    success = true,
                    message = "Connected · ${response.text.take(40)}",
                    latencyMs = System.currentTimeMillis() - started,
                    model = testConfig.effectiveModel
                )
            } catch (t: Throwable) {
                ProviderTestResult(
                    success = false,
                    message = t.toUserMessage("Connection failed").userMessage,
                    latencyMs = System.currentTimeMillis() - started,
                    model = config.effectiveModel
                )
            }
        }

    private fun parseResponse(json: JsonObject, model: String): ProviderResponse {
        val candidates = json.getAsJsonArray("candidates")
        if (candidates == null || candidates.size() == 0) {
            val blockReason = runCatching {
                json.getAsJsonObject("promptFeedback")?.get("blockReason")?.asString
            }.getOrNull()
            throw AiRequestException(
                if (blockReason != null) "Request blocked by Gemini ($blockReason)" else "Gemini returned no candidates"
            )
        }
        val parts = candidates[0].asJsonObject
            .getAsJsonObject("content")
            ?.getAsJsonArray("parts")
        val text = parts?.joinToString("\n") { part ->
            runCatching { part.asJsonObject.get("text")?.asString }.getOrNull().orEmpty()
        }.orEmpty()
        if (text.isBlank()) {
            throw AiRequestException("Gemini returned an empty response")
        }
        val usage = runCatching {
            json.getAsJsonObject("usageMetadata")?.get("totalTokenCount")?.asInt
        }.getOrNull()
        return ProviderResponse(text = text.trim(), model = model, usageTokens = usage)
    }
}
