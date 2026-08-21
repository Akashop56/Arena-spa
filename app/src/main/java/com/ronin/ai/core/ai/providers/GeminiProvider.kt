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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.URLEncoder

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

    /**
     * Converts the neutral message list into Gemini's `contents` format.
     * System turns become `systemInstruction`; consecutive same-role turns are
     * merged because the API rejects duplicated roles.
     */
    private fun buildBody(
        messages: List<ProviderMessage>,
        temperature: Float
    ): JsonObject {
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

        return JsonObject().apply {
            add("contents", contents)
            systemInstruction?.let { add("systemInstruction", it) }
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", temperature.toDouble())
                addProperty("maxOutputTokens", Constants.MAX_OUTPUT_TOKENS)
                addProperty("candidateCount", 1)
            })
        }
    }

    private fun endpoint(model: String, apiKey: String, method: String): String {
        val key = URLEncoder.encode(apiKey, "UTF-8")
        return "https://generativelanguage.googleapis.com/v1beta/models/$model:$method?key=$key"
    }

    override suspend fun complete(
        config: AiProviderConfig,
        messages: List<ProviderMessage>,
        temperature: Float
    ): ProviderResponse = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            throw AiRequestException("No API key configured — add one in Settings → AI Providers")
        }
        val model = config.effectiveModel
        val url = endpoint(model, config.apiKey, "generateContent")
        val body = buildBody(messages, temperature)

        try {
            withRetry {
                val response = api.generateContent(url, body)
                parseResponse(response, model)
            }
        } catch (t: AiRequestException) {
            throw t
        } catch (t: Throwable) {
            throw t.toUserMessage("Gemini could not be reached")
        }
    }

    /**
     * Gemini streams newline-delimited SSE chunks from `streamGenerateContent`.
     * Falls back to the buffered call when streaming is unavailable.
     */
    override fun stream(
        config: AiProviderConfig,
        messages: List<ProviderMessage>,
        temperature: Float
    ): Flow<String> = flow {
        if (config.apiKey.isBlank()) {
            throw AiRequestException("No API key configured — add one in Settings → AI Providers")
        }
        val model = config.effectiveModel
        val url = endpoint(model, config.apiKey, "streamGenerateContent") + "&alt=sse"
        val body = buildBody(messages, temperature)

        val responseBody = try {
            api.streamGenerateContent(url, body)
        } catch (t: Throwable) {
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
                if (payload.isEmpty() || payload == "[DONE]") continue
                val text = runCatching {
                    gson.fromJson(payload, JsonObject::class.java)
                        ?.getAsJsonArray("candidates")
                        ?.takeIf { it.size() > 0 }
                        ?.get(0)?.asJsonObject
                        ?.getAsJsonObject("content")
                        ?.getAsJsonArray("parts")
                        ?.joinToString("") { part ->
                            runCatching { part.asJsonObject.get("text")?.asString }
                                .getOrNull().orEmpty()
                        }
                }.getOrNull()
                if (!text.isNullOrEmpty()) {
                    emittedAnything = true
                    emit(text)
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
