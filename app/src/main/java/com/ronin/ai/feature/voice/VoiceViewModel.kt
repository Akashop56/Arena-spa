package com.ronin.ai.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.device.VoiceService
import com.ronin.ai.core.domain.model.VoiceLanguage
import com.ronin.ai.core.domain.repository.SettingsRepository
import com.ronin.ai.core.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceService: VoiceService,
    private val sendMessage: SendMessageUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _language = MutableStateFlow(VoiceLanguage.EN_US.code)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _providerLabel = MutableStateFlow("System")
    val providerLabel: StateFlow<String> = _providerLabel.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.voiceSettings.collect { settings ->
                _providerLabel.value = settings.provider.displayName
            }
        }
    }

    fun toggleLanguage() {
        _language.value = if (_language.value.startsWith("hi")) {
            VoiceLanguage.EN_US.code
        } else {
            VoiceLanguage.HI_IN.code
        }
    }

    fun startListening() {
        if (_isListening.value || _isThinking.value) return
        val started = voiceService.listen(
            language = _language.value,
            onPartial = { result -> _partialText.value = result.text },
            onResult = { result ->
                _isListening.value = false
                _partialText.value = ""
                if (result.text.isNotBlank()) {
                    _lastTranscript.value = result.text
                    processText(result.text)
                }
            },
            onError = { message ->
                _isListening.value = false
                _partialText.value = ""
                _error.value = message
            }
        )
        if (started) {
            _isListening.value = true
            _error.value = null
        }
    }

    fun stopListening() {
        voiceService.stopSpeaking()
        _isListening.value = false
        _partialText.value = ""
    }

    fun testVoice() {
        _error.value = null
        voiceService.testVoice()
    }

    fun dismissError() {
        _error.value = null
    }

    private fun processText(text: String) {
        viewModelScope.launch {
            _isThinking.value = true
            try {
                val reply = sendMessage.invoke(text)
                _isSpeaking.value = true
                voiceService.speak(reply.reply, _language.value) {
                    _isSpeaking.value = false
                }
            } catch (t: Throwable) {
                _error.value = t.message ?: "Something went wrong"
            } finally {
                _isThinking.value = false
            }
        }
    }
}
