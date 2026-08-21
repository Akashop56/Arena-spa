package com.ronin.ai.core.ai.providers

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Minimal dynamic Retrofit API used by every AI provider. The base URL is
 * supplied per call, so one interface covers OpenAI-compatible endpoints
 * (OpenAI, Groq, custom) and the Gemini REST API.
 */
interface AiApi {

    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body body: JsonObject
    ): JsonObject

    @POST
    suspend fun generateContent(
        @Url url: String,
        @Body body: JsonObject
    ): JsonObject

    /**
     * Server-sent-events variant of [chatCompletions]. The body is streamed so
     * tokens can be surfaced as they arrive instead of buffering the whole
     * reply in memory (important on low-end devices).
     */
    @Streaming
    @POST
    suspend fun chatCompletionsStream(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body body: JsonObject
    ): ResponseBody

    /** Server-sent-events variant of the Gemini generateContent endpoint. */
    @Streaming
    @POST
    suspend fun streamGenerateContent(
        @Url url: String,
        @Body body: JsonObject
    ): ResponseBody
}
