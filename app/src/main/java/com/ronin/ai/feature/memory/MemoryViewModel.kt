package com.ronin.ai.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.MemoryItem
import com.ronin.ai.core.domain.model.MemoryType
import com.ronin.ai.core.domain.usecase.MemoryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryUseCases: MemoryUseCases
) : ViewModel() {

    private val _filter = MutableStateFlow<MemoryType?>(null)
    val filter: StateFlow<MemoryType?> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val items: StateFlow<List<MemoryItem>> =
        combine(_filter, _query) { type, q -> type to q }
            .flatMapLatest { (type, q) ->
                when {
                    q.isNotBlank() -> memoryUseCases.search(q)
                    type != null -> memoryUseCases.byType(type)
                    else -> memoryUseCases.all()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(type: MemoryType?) {
        _filter.value = type
    }

    fun onQueryChange(value: String) {
        _query.value = value
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
}
