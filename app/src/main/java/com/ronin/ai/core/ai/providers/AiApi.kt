package com.ronin.ai.core.ai.providers

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
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
}
