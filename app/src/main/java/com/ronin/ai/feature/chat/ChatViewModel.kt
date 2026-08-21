package com.ronin.ai.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.ChatMessage
import com.ronin.ai.core.domain.model.PipelineStage
import com.ronin.ai.core.domain.repository.ConversationRepository
import com.ronin.ai.core.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    /**
     * Persisted history straight from Room. Using stateIn (instead of a manual
     * collect into a MutableStateFlow) means the DB flow is only observed while
     * the UI is subscribed — no leaked collector when the screen is away.
     */
    val messages: StateFlow<List<ChatMessage>> = conversationRepository.messages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    /** Live partial reply while the model streams; empty when idle. */
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    val stage: StateFlow<PipelineStage?> = sendMessage.stage

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var sendJob: Job? = null

    fun onInputChange(value: String) {
        _input.value = value
    }

    fun send() {
        val text = _input.value.trim()
        if (text.isEmpty() || _isThinking.value) return
        _input.value = ""
        sendJob = viewModelScope.launch {
            _isThinking.value = true
            _error.value = null
            _streamingText.value = ""
            try {
                sendMessage.invoke(text) { chunk ->
                    _streamingText.value += chunk
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _error.value = t.message ?: "Something went wrong"
            } finally {
                // The final reply is now persisted in Room and rendered from
                // `messages`, so the transient buffer must be cleared.
                _streamingText.value = ""
                _isThinking.value = false
            }
        }
    }

    /** Stops an in-flight generation without losing what was already stored. */
    fun stopGenerating() {
        sendJob?.cancel()
        sendJob = null
        _streamingText.value = ""
        _isThinking.value = false
    }

    fun clearConversation() {
        viewModelScope.launch { conversationRepository.clearAll() }
    }

    fun dismissError() {
        _error.value = null
    }
}
