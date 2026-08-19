package com.ronin.ai.core.device

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.ronin.ai.core.domain.model.VoiceProviderType
import com.ronin.ai.core.domain.model.VoiceRecognitionResult
import com.ronin.ai.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single entry point for all speech I/O. Picks the configured voice
 * provider and transparently falls back to the on-device system voice
 * whenever a cloud provider is missing a key or fails — offline fallback
 * is built into the layer, not into each screen.
 */
@Singleton
class VoiceService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val systemVoiceEngine: SystemVoiceEngine,
    private val remoteVoiceEngine: RemoteVoiceEngine
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var player: MediaPlayer? = null

    /** Speaks [text]. Falls back to system TTS when the cloud provider fails. */
    fun speak(text: String, language: String? = null, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }
        stopSpeaking()
        scope.launch {
            val settings = settingsRepository.getVoiceSettings()
            val lang = language ?: settings.language
            val useSystem = settings.provider == VoiceProviderType.SYSTEM || !settings.hasKey
            if (useSystem) {
                systemVoiceEngine.speak(text, lang, settings.speed, settings.pitch, onDone)
                return@launch
            }
            val audio = runCatching { remoteVoiceEngine.synthesize(settings, text) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
            if (audio != null) {
                playAudio(audio, onDone)
            } else {
                // Offline fallback: the system voice always works.
                systemVoiceEngine.speak(text, lang, settings.speed, settings.pitch, onDone)
            }
        }
    }

    fun stopSpeaking() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        systemVoiceEngine.stop()
    }

    fun isSystemVoiceReady(): Boolean = systemVoiceEngine.isReady()

    fun isRecognitionAvailable(): Boolean = systemVoiceEngine.isRecognitionAvailable()

    /** Starts system speech recognition. */
    fun listen(
        language: String,
        onPartial: (VoiceRecognitionResult) -> Unit,
        onResult: (VoiceRecognitionResult) -> Unit,
        onError: (String) -> Unit
    ): Boolean = systemVoiceEngine.listen(language, onPartial, onResult, onError)

    /** Speaks a short sample using current settings (voice settings screen). */
    fun testVoice(onDone: (() -> Unit)? = null) {
        scope.launch {
            val settings = settingsRepository.getVoiceSettings()
            val sample = if (settings.language.startsWith("hi")) {
                "नमस्ते! मैं रोनिन हूँ, आपका निजी सहायक। Hello! I am Ronin, your personal assistant."
            } else {
                "Hello! I am Ronin, your personal AI assistant. This is how my voice sounds."
            }
            speak(sample, settings.language, onDone)
        }
    }

    private fun playAudio(bytes: ByteArray, onDone: (() -> Unit)?) {
        runCatching {
            val file = File(context.cacheDir, "ronin_tts_${System.currentTimeMillis()}.mp3")
            file.writeBytes(bytes)
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            mp.setOnCompletionListener {
                runCatching { it.release() }
                runCatching { file.delete() }
                if (player === it) player = null
                onDone?.invoke()
            }
            mp.setOnErrorListener { _, _, _ ->
                runCatching { it.release() }
                runCatching { file.delete() }
                if (player === it) player = null
                onDone?.invoke()
                true
            }
            mp.start()
        }.onFailure {
            onDone?.invoke()
        }
    }
}
