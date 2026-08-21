package com.ronin.ai.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType
import com.ronin.ai.core.domain.repository.SettingsRepository
import com.ronin.ai.core.domain.usecase.MemoryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Counts shown above the memory list. */
data class MemoryStats(
    val total: Int = 0,
    val byType: Map<MemoryType, Int> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryUseCases: MemoryUseCases,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<MemoryType?>(null)
    val filter: StateFlow<MemoryType?> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Master switch — mirrors the value the AI engine reads. */
    val memoryEnabled: StateFlow<Boolean> = settingsRepository.memoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val items: StateFlow<List<MemoryItem>> =
        combine(
            _filter,
            // Debounce typing so each keystroke doesn't hit the database.
            _query.debounce { if (it.isBlank()) 0L else 250L }
        ) { type, q -> type to q }
            .flatMapLatest { (type, q) ->
                when {
                    q.isNotBlank() -> memoryUseCases.search(q)
                    type != null -> memoryUseCases.byType(type)
                    else -> memoryUseCases.all()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Live totals derived from the full memory set. */
    val stats: StateFlow<MemoryStats> = memoryUseCases.all()
        .map { all ->
            MemoryStats(
                total = all.size,
                byType = all.groupingBy { it.type }.eachCount()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryStats())

    fun setFilter(type: MemoryType?) {
        _filter.value = type
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun setMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMemoryEnabled(enabled) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { memoryUseCases.delete(id) }
    }

    fun save(item: MemoryItem) {
        viewModelScope.launch { memoryUseCases.save(item) }
    }

    fun clearAll() {
        viewModelScope.launch { memoryUseCases.clearAll() }
    }

    fun deleteByType(type: MemoryType) {
        viewModelScope.launch { memoryUseCases.deleteByType(type) }
    }
}
