package com.ronin.ai.core.device

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.ronin.ai.core.domain.model.VoiceProviderType
import com.ronin.ai.core.domain.model.VoiceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud TTS providers (ElevenLabs, Google Cloud, Azure Speech, custom
 * OpenAI-compatible voice endpoint). Speech recognition always uses the
 * on-device system recognizer — cloud STT is a future extension point.
 */
@Singleton
class RemoteVoiceEngine @Inject constructor(
    private val okHttp: OkHttpClient,
    private val gson: Gson
) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val ssmlMedia = "application/ssml+xml".toMediaType()

    suspend fun synthesize(settings: VoiceSettings, text: String): ByteArray? =
        withContext(Dispatchers.IO) {
            when (settings.provider) {
                VoiceProviderType.SYSTEM -> null
                VoiceProviderType.ELEVENLABS -> elevenLabs(settings, text)
                VoiceProviderType.GOOGLE_CLOUD -> googleCloud(settings, text)
                VoiceProviderType.AZURE -> azure(settings, text)
                VoiceProviderType.CUSTOM -> custom(settings, text)
            }
        }

    private fun elevenLabs(settings: VoiceSettings, text: String): ByteArray? {
        if (settings.apiKey.isBlank()) return null
        val voiceId = settings.voiceId.ifBlank { "21m00Tcm4TlvDq8ikWAM" }
        val body = JsonObject().apply {
            addProperty("text", text)
            addProperty("model_id", settings.model.ifBlank { "eleven_multilingual_v2" })
            add("voice_settings", JsonObject().apply {
                addProperty("stability", 0.5)
                addProperty("similarity_boost", 0.75)
            })
        }
        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
            .addHeader("xi-api-key", settings.apiKey)
            .addHeader("Accept", "audio/mpeg")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        return executeBytes(request)
    }

    private fun googleCloud(settings: VoiceSettings, text: String): ByteArray? {
        if (settings.apiKey.isBlank()) return null
        val body = JsonObject().apply {
            add("input", JsonObject().apply { addProperty("text", text) })
            add("voice", JsonObject().apply {
                addProperty("languageCode", settings.language)
                if (settings.voiceId.isNotBlank()) addProperty("name", settings.voiceId)
            })
            add("audioConfig", JsonObject().apply {
                addProperty("audioEncoding", "MP3")
                addProperty("speakingRate", settings.speed.toDouble())
                addProperty("pitch", settings.pitch.toDouble())
            })
        }
        val request = Request.Builder()
            .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=${settings.apiKey}")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        return executeBytes(request)?.let { bytes ->
            // Google returns JSON { "audioContent": "<base64>" }
            runCatching {
                val json = String(bytes, Charsets.UTF_8)
                val content = gson.fromJson(json, JsonObject::class.java)
                    .get("audioContent")?.asString ?: return null
                Base64.decode(content, Base64.DEFAULT)
            }.getOrNull()
        }
    }

    private fun azure(settings: VoiceSettings, text: String): ByteArray? {
        if (settings.apiKey.isBlank()) return null
        val region = settings.model.ifBlank { "eastus" }
        val voice = settings.voiceId.ifBlank {
            if (settings.language.startsWith("hi")) "hi-IN-MadhurNeural" else "en-US-AriaNeural"
        }
        val ssml = buildString {
            append("<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' ")
            append("xml:lang='").append(settings.language).append("'>")
            append("<voice name='").append(voice).append("'>")
            append(text)
            append("</voice></speak>")
        }
        val request = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .addHeader("Ocp-Apim-Subscription-Key", settings.apiKey)
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .addHeader("User-Agent", "RoninAI")
            .post(ssml.toRequestBody(ssmlMedia))
            .build()
        return executeBytes(request)
    }

    private fun custom(settings: VoiceSettings, text: String): ByteArray? {
        if (settings.apiKey.isBlank()) return null
        val baseUrl = settings.endpoint
        if (baseUrl.isBlank()) return null
        val body = JsonObject().apply {
            addProperty("model", settings.model.ifBlank { "tts-1" })
            addProperty("input", text)
            addProperty("voice", settings.voiceId.ifBlank { "alloy" })
            addProperty("response_format", "mp3")
        }
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/audio/speech")
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        return executeBytes(request)
    }

    private fun executeBytes(request: Request): ByteArray? = runCatching {
        okHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.bytes()
        }
    }.getOrNull()

    companion object {
        /** A client used only for short voice calls. */
        fun shortTimeoutClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
