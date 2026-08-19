package com.ronin.ai.feature.skills

import androidx.lifecycle.ViewModel
import com.ronin.ai.core.ai.tools.ToolRegistry
import com.ronin.ai.core.domain.model.ExperienceItem
import com.ronin.ai.core.domain.model.ToolDefinition
import com.ronin.ai.core.domain.repository.ExperienceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SkillsViewModel @Inject constructor(
    toolRegistry: ToolRegistry,
    experienceRepository: ExperienceRepository
) : ViewModel() {

    private val _skills = MutableStateFlow(toolRegistry.definitions())
    val skills: StateFlow<List<ToolDefinition>> = _skills.asStateFlow()

    val experiences: StateFlow<List<ExperienceItem>> = experienceRepository.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
