package com.ronin.ai.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.device.VoiceService
import com.ronin.ai.core.domain.model.VoiceLanguage
import com.ronin.ai.core.domain.repository.SettingsRepository
import com.ronin.ai.core.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Microphone lifecycle, surfaced to the UI as an explicit state machine. */
enum class MicState { IDLE, LISTENING, PROCESSING, SPEAKING, UNAVAILABLE }

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceService: VoiceService,
    private val sendMessage: SendMessageUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _micState = MutableStateFlow(MicState.IDLE)
    val micState: StateFlow<MicState> = _micState.asStateFlow()

    /** Live input level 0..1 used by the waveform while listening. */
    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _lastReply = MutableStateFlow("")
    val lastReply: StateFlow<String> = _lastReply.asStateFlow()

    private val _language = MutableStateFlow(VoiceLanguage.EN_US.code)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _providerLabel = MutableStateFlow("System")
    val providerLabel: StateFlow<String> = _providerLabel.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var processJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.voiceSettings.collect { settings ->
                _providerLabel.value = settings.provider.displayName
            }
        }
        viewModelScope.launch {
            // Start from the configured language instead of always English.
            val configured = settingsRepository.getVoiceSettings().language
            if (configured.isNotBlank()) _language.value = configured
        }
        if (!voiceService.isRecognitionAvailable()) {
            _micState.value = MicState.UNAVAILABLE
        }
    }

    fun setLanguage(code: String) {
        if (_language.value == code) return
        _language.value = code
        viewModelScope.launch {
            // Persist so TTS and STT agree on the language across screens.
            val current = settingsRepository.getVoiceSettings()
            settingsRepository.saveVoiceSettings(current.copy(language = code))
        }
    }

    fun toggleLanguage() {
        setLanguage(
            if (_language.value.startsWith("hi")) VoiceLanguage.EN_US.code
            else VoiceLanguage.HI_IN.code
        )
    }

    fun startListening() {
        if (_micState.value == MicState.LISTENING || _micState.value == MicState.PROCESSING) return
        if (!voiceService.isRecognitionAvailable()) {
            _micState.value = MicState.UNAVAILABLE
            _error.value = "Speech recognition is not available on this device"
            return
        }
        _error.value = null
        _partialText.value = ""
        voiceService.stopSpeaking()

        val started = voiceService.listen(
            language = _language.value,
            onPartial = { result -> _partialText.value = result.text },
            onResult = { result ->
                _micLevel.value = 0f
                _partialText.value = ""
                if (result.text.isNotBlank()) {
                    _lastTranscript.value = result.text
                    processText(result.text)
                } else {
                    _micState.value = MicState.IDLE
                }
            },
            onError = { message ->
                _micLevel.value = 0f
                _partialText.value = ""
                _micState.value = MicState.IDLE
                _error.value = message
            },
            onRms = { level -> _micLevel.value = level },
            onReady = { _micState.value = MicState.LISTENING },
            onEndOfSpeech = {
                _micLevel.value = 0f
                if (_micState.value == MicState.LISTENING) {
                    _micState.value = MicState.PROCESSING
                }
            }
        )
        if (started) {
            _micState.value = MicState.LISTENING
        }
    }

    /** Cancels the microphone session (previously this stopped playback instead). */
    fun stopListening() {
        voiceService.stopListening()
        _micLevel.value = 0f
        _partialText.value = ""
        if (_micState.value == MicState.LISTENING) _micState.value = MicState.IDLE
    }

    /** Stops whatever RONIN is currently doing: listening, thinking or talking. */
    fun stopAll() {
        processJob?.cancel()
        processJob = null
        voiceService.stopListening()
        voiceService.stopSpeaking()
        _micLevel.value = 0f
        _partialText.value = ""
        _micState.value = MicState.IDLE
    }

    fun testVoice() {
        _error.value = null
        _micState.value = MicState.SPEAKING
        voiceService.testVoice { _micState.value = MicState.IDLE }
    }

    fun dismissError() {
        _error.value = null
    }

    private fun processText(text: String) {
        processJob?.cancel()
        processJob = viewModelScope.launch {
            _micState.value = MicState.PROCESSING
            try {
                val reply = sendMessage.invoke(text)
                _lastReply.value = reply.reply
                _micState.value = MicState.SPEAKING
                voiceService.speak(reply.reply, _language.value) {
                    _micState.value = MicState.IDLE
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _error.value = t.message ?: "Something went wrong"
                _micState.value = MicState.IDLE
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Release the mic/TTS when the screen goes away — otherwise the
        // recognizer can hold the audio focus indefinitely.
        voiceService.stopListening()
        voiceService.stopSpeaking()
    }
}
