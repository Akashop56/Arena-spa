package com.ronin.ai.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.ChatMessage
import com.ronin.ai.core.domain.model.PipelineStage
import com.ronin.ai.core.domain.repository.ConversationRepository
import com.ronin.ai.core.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    val stage: StateFlow<PipelineStage?> = sendMessage.stage

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            conversationRepository.messages().collect { _messages.value = it }
        }
    }

    fun onInputChange(value: String) {
        _input.value = value
    }

    fun send() {
        val text = _input.value.trim()
        if (text.isEmpty() || _isThinking.value) return
        _input.value = ""
        viewModelScope.launch {
            _isThinking.value = true
            _error.value = null
            try {
                sendMessage.invoke(text)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Something went wrong"
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun clearConversation() {
        viewModelScope.launch { conversationRepository.clearAll() }
    }

    fun dismissError() {
        _error.value = null
    }
}
