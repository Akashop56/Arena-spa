package com.ronin.ai.feature.providers

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.design.components.ErrorBanner
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.StatusChip
import com.ronin.ai.core.design.components.SwitchRow
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninWarning
import com.ronin.ai.core.domain.model.AiProviderType

@Composable
fun AiProvidersScreen(
    onBack: () -> Unit,
    onEditProvider: (AiProviderType) -> Unit,
    viewModel: AiProvidersViewModel = hiltViewModel()
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val testingType by viewModel.testingType.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Re-read configs when returning from the provider editor, otherwise the
    // list still shows the values from before the edit.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    RoninBackground {
        Column(Modifier.fillMaxSize()) {
            RoninHeader(
                title = "AI Providers",
                subtitle = "brain",
                onBack = onBack
            )

            if (error != null) {
                ErrorBanner(
                    message = error ?: "",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(providers, key = { it.config.type }) { state ->
                    ProviderCard(
                        state = state,
                        testing = testingType == state.config.type,
                        onToggle = { viewModel.setEnabled(state.config.type, it) },
                        onTest = { viewModel.testConnection(state.config.type) },
                        onEdit = { onEditProvider(state.config.type) },
                        onDeleteKey = { viewModel.deleteKey(state.config.type) },
                        onSetDefault = { viewModel.setDefault(state.config.type) }
                    )
                }

                item {
                    testResult?.let { (type, result) ->
                        if (result.success) {
                            ErrorBanner(
                                "✓ ${type.displayName}: ${result.message} (${result.latencyMs} ms)",
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            ErrorBanner(
                                "✗ ${type.displayName}: ${result.message}",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    state: ProviderUiState,
    testing: Boolean,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDeleteKey: () -> Unit,
    onSetDefault: () -> Unit
) {
    val config = state.config
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = config.enabled) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        config.type.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        config.type.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoninTextSecondary
                    )
                }
                if (state.isDefault) {
                    StatusChip("DEFAULT", RoninCyan)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    when {
                        testing -> "TESTING…"
                        config.enabled && config.hasKey -> "CONNECTED"
                        config.enabled -> "NO KEY"
                        else -> "DISABLED"
                    },
                    when {
                        testing -> RoninWarning
                        config.enabled && config.hasKey -> RoninSuccess
                        else -> RoninError
                    }
                )
            }

            Text(
                "Model: ${config.effectiveModel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (config.hasKey) "API key: stored securely (••••••••)"
                else "API key: not set",
                style = MaterialTheme.typography.labelMedium,
                color = if (config.hasKey) RoninSuccess else RoninWarning
            )

            SwitchRow(
                title = "Enabled",
                checked = config.enabled,
                onCheckedChange = onToggle
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradientButton(
                    text = if (testing) "Testing…" else "Test",
                    onClick = onTest,
                    icon = Icons.Rounded.Refresh,
                    enabled = !testing,
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = "Edit",
                    onClick = onEdit,
                    icon = Icons.Rounded.Edit,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradientButton(
                    text = if (state.isDefault) "Default" else "Set default",
                    onClick = onSetDefault,
                    icon = Icons.Rounded.Star,
                    enabled = !state.isDefault,
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = "Delete key",
                    onClick = onDeleteKey,
                    icon = Icons.Rounded.DeleteOutline,
                    enabled = config.hasKey,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
