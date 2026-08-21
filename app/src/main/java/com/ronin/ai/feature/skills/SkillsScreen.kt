package com.ronin.ai.feature.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.common.TimeFormat
import com.ronin.ai.core.design.components.EmptyState
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.SectionHeader
import com.ronin.ai.core.design.components.StatusChip
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninAmber
import com.ronin.ai.core.domain.model.ExperienceCategory
import com.ronin.ai.core.domain.model.ExperienceItem
import com.ronin.ai.core.domain.model.ToolDefinition

@Composable
fun SkillsScreen(
    onBack: () -> Unit,
    viewModel: SkillsViewModel = hiltViewModel()
) {
    val skills by viewModel.skills.collectAsStateWithLifecycle()
    val experiences by viewModel.experiences.collectAsStateWithLifecycle()

    val fixes = experiences.filter { it.category == ExperienceCategory.FIX }
    val errors = experiences.filter { it.category == ExperienceCategory.ERROR && !it.resolved }

    RoninBackground {
        Column(Modifier.fillMaxSize()) {
            RoninHeader(title = "Skills", subtitle = "tool framework", onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionHeader(title = "Installed skills (${skills.size})")
                }
                items(skills, key = { it.id }) { skill ->
                    SkillCard(skill)
                }

                item {
                    SectionHeader(
                        title = "Learned solutions (${fixes.size})",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (fixes.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "No lessons yet",
                            subtitle = "As RONIN works, successful fixes and solutions are recorded here automatically."
                        )
                    }
                } else {
                    items(fixes.take(8), key = { it.id }) { fix ->
                        ExperienceCard(fix, accent = RoninSuccess)
                    }
                }

                item {
                    SectionHeader(
                        title = "Open issues (${errors.size})",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (errors.isNotEmpty()) {
                    items(errors.take(8), key = { it.id }) { error ->
                        ExperienceCard(error, accent = RoninError)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillCard(skill: ToolDefinition) {
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = RoninCyan,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    skill.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoninTextSecondary
                )
            }
            StatusChip(skill.category.label, RoninAmber)
        }
    }
}

@Composable
private fun ExperienceCard(item: ExperienceItem, accent: Color) {
    val icon: ImageVector = when (item.category) {
        ExperienceCategory.ERROR -> Icons.Rounded.ErrorOutline
        else -> Icons.Rounded.CheckCircle
    }
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(item.detail, style = MaterialTheme.typography.bodyMedium, color = RoninTextSecondary)
                Text(
                    TimeFormat.relative(item.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = RoninTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
