package com.ronin.ai.core.ai.providers

import com.google.gson.Gson
import com.ronin.ai.core.domain.model.AiProviderType
import javax.inject.Inject
import javax.inject.Singleton

/** Creates the right provider implementation for a provider type. */
@Singleton
class AiProviderFactory @Inject constructor(
    private val api: AiApi,
    private val gson: Gson
) {

    fun forType(type: AiProviderType): AiProvider = when (type) {
        AiProviderType.GEMINI -> GeminiProvider(api, gson)
        AiProviderType.GROQ,
        AiProviderType.OPENAI,
        AiProviderType.CUSTOM -> OpenAiCompatibleProvider(api, gson, type)
    }
}
