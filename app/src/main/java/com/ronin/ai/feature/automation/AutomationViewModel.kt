package com.ronin.ai.feature.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.Routine
import com.ronin.ai.core.domain.model.RoutineAction
import com.ronin.ai.core.domain.model.RoutineHistoryEntry
import com.ronin.ai.core.domain.usecase.RoutineUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutomationViewModel @Inject constructor(
    private val routineUseCases: RoutineUseCases
) : ViewModel() {

    val routines: StateFlow<List<Routine>> = routineUseCases.routines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<RoutineHistoryEntry>> = routineUseCases.history()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { routineUseCases.setEnabled(id, enabled) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { routineUseCases.delete(id) }
    }

    fun runNow(id: Long) {
        viewModelScope.launch { routineUseCases.runNow(id) }
    }

    fun save(id: Long?, name: String, triggerPhrase: String, actions: List<RoutineAction>, enabled: Boolean) {
        viewModelScope.launch {
            routineUseCases.save(id, name, triggerPhrase, actions, enabled)
        }
    }
}
