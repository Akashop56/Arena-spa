package com.ronin.ai.core.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ronin.ai.core.domain.model.VoiceRecognitionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android system voice: on-device TextToSpeech + SpeechRecognizer.
 * Works with zero API keys and falls back gracefully when a language pack
 * is missing (the recogniser simply reports an error, which callers surface).
 */
@Singleton
class SystemVoiceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var ttsReady = false

    private var pendingText: String? = null
    private var pendingLanguage = "en-US"
    private var pendingSpeed = 1f
    private var pendingPitch = 1f
    private var pendingDone: (() -> Unit)? = null
    private var doneCallback: (() -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null
    private var recognizerActive = false

    init {
        mainHandler.post { initTts() }
    }

    private fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            mainHandler.post {
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    flushPending()
                }
            }
        }.also { engine ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        val cb = doneCallback
                        doneCallback = null
                        cb?.invoke()
                    }
                }
                @Deprecated("Deprecated in Android API 21+")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        val cb = doneCallback
                        doneCallback = null
                        cb?.invoke()
                    }
                }
            })
        }
    }

    private fun flushPending() {
        val text = pendingText ?: return
        pendingText = null
        doSpeak(text, pendingLanguage, pendingSpeed, pendingPitch, pendingDone)
    }

    /** Speaks [text] using the given language tag (e.g. "en-US", "hi-IN"). */
    fun speak(
        text: String,
        language: String,
        speed: Float,
        pitch: Float,
        onDone: (() -> Unit)? = null
    ) {
        mainHandler.post {
            if (!ttsReady) {
                pendingText = text
                pendingLanguage = language
                pendingSpeed = speed
                pendingPitch = pitch
                pendingDone = onDone
                return@post
            }
            doSpeak(text, language, speed, pitch, onDone)
        }
    }

    private fun doSpeak(
        text: String,
        language: String,
        speed: Float,
        pitch: Float,
        onDone: (() -> Unit)?
    ) {
        val engine = tts ?: return
        // setLanguage returns a status: if the requested language has no voice
        // data we fall back to the device default rather than failing silently.
        val requested = Locale.forLanguageTag(language)
        val status = runCatching { engine.setLanguage(requested) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (status == TextToSpeech.LANG_MISSING_DATA || status == TextToSpeech.LANG_NOT_SUPPORTED) {
            runCatching { engine.setLanguage(Locale.getDefault()) }
        }
        engine.setSpeechRate(speed.coerceIn(0.5f, 2f))
        engine.setPitch(pitch.coerceIn(0.5f, 2f))
        doneCallback = onDone
        val utteranceId = "ronin_${System.currentTimeMillis()}"
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            doneCallback = null
            onDone?.invoke()
        }
    }

    /** Releases the current recognizer session. Safe to call repeatedly. */
    private fun destroyRecognizer() {
        recognizerActive = false
        val sr = recognizer
        recognizer = null
        runCatching { sr?.cancel() }
        runCatching { sr?.destroy() }
    }

    /** Stops speech playback only — recognition is left untouched. */
    fun stopSpeaking() {
        mainHandler.post {
            runCatching { tts?.stop() }
            doneCallback = null
            pendingText = null
            pendingDone = null
        }
    }

    /** Stops an active listening session only. */
    fun stopListening() {
        mainHandler.post {
            runCatching { recognizer?.stopListening() }
            destroyRecognizer()
        }
    }

    fun isListening(): Boolean = recognizerActive

    /** Stops both playback and recognition. */
    fun stop() {
        mainHandler.post {
            runCatching { tts?.stop() }
            doneCallback = null
            pendingText = null
            pendingDone = null
            destroyRecognizer()
        }
    }

    fun shutdown() {
        mainHandler.post {
            destroyRecognizer()
            runCatching { tts?.stop() }
            runCatching { tts?.shutdown() }
            tts = null
            ttsReady = false
        }
    }

    fun isReady(): Boolean = ttsReady

    // ------------------------------------------------------------ recognition
    fun isRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Starts a listening session. Results are delivered on the main thread.
     * Returns false if recognition is unavailable.
     */
    fun listen(
        language: String,
        onPartial: (VoiceRecognitionResult) -> Unit,
        onResult: (VoiceRecognitionResult) -> Unit,
        onError: (String) -> Unit,
        onRms: ((Float) -> Unit)? = null,
        onReady: (() -> Unit)? = null,
        onEndOfSpeech: (() -> Unit)? = null
    ): Boolean {
        if (!isRecognitionAvailable()) {
            onError("Speech recognition is not available on this device")
            return false
        }
        mainHandler.post {
            // Never speak over ourselves: stop any playback before listening.
            runCatching { tts?.stop() }
            if (recognizerActive) {
                // Recover instead of refusing: tear the old session down and
                // start a fresh one. A stale recognizer was the main cause of
                // the mic becoming permanently unresponsive.
                destroyRecognizer()
            }
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = sr
            recognizerActive = true

            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    mainHandler.post { onReady?.invoke() }
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) {
                    // Normalise the (roughly) -2..10 dB range into 0..1 so the
                    // UI can drive a live microphone level indicator.
                    val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    onRms?.invoke(level)
                }
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    mainHandler.post { onEndOfSpeech?.invoke() }
                }

                override fun onError(error: Int) {
                    destroyRecognizer()
                    mainHandler.post { onError(errorLabel(error)) }
                }

                override fun onResults(results: Bundle?) {
                    destroyRecognizer()
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    mainHandler.post {
                        onResult(
                            VoiceRecognitionResult(
                                text = text,
                                language = language,
                                isPartial = false
                            )
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotBlank()) {
                        mainHandler.post {
                            onPartial(
                                VoiceRecognitionResult(
                                    text = text,
                                    language = language,
                                    isPartial = true
                                )
                            )
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            sr.startListening(intent)
        }
        return true
    }

    private fun errorLabel(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Recognition client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't hear anything clear — please try again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
        SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear any speech — try again"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language pack not available on this device"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests — try again in a moment"
        else -> "Recognition error ($code)"
    }
}
