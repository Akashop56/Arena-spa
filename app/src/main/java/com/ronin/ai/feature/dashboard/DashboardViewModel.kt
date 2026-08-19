package com.ronin.ai.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.domain.model.DashboardData
import com.ronin.ai.core.domain.usecase.DashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardUseCase: DashboardUseCase
) : ViewModel() {

    val data: StateFlow<DashboardData> = dashboardUseCase.data

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { dashboardUseCase.refresh() }
    }
}
