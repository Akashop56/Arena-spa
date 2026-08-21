package com.ronin.ai.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.DashboardData
import com.ronin.ai.core.domain.usecase.DashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardUseCase: DashboardUseCase
) : ViewModel() {

    /**
     * Reactive dashboard state — recomputes whenever memories, routines,
     * experiences or the conversation change, and stops collecting when no UI
     * is subscribed.
     */
    val data: StateFlow<DashboardData> = dashboardUseCase.observe()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardData(assistantName = "RONIN")
        )

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Manual refresh for values that are not table-backed (e.g. battery). */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            runCatching { dashboardUseCase.snapshot() }
            _refreshing.value = false
        }
    }
}
