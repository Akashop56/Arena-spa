package com.ronin.ai.feature.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.design.components.ErrorBanner
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.OptionChip
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.RoninTextField
import com.ronin.ai.core.design.components.SwitchRow
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.domain.model.AiProviderType

@Composable
fun AiProviderEditScreen(
    onBack: () -> Unit,
    viewModel: AiProviderEditViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    RoninBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RoninHeader(
                title = config.type.displayName,
                subtitle = "provider",
                onBack = onBack
            )

            if (error != null) {
                ErrorBanner(error ?: "")
            }

            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SwitchRow(
                        title = "Enabled",
                        subtitle = "Use this provider in the AI brain",
                        checked = config.enabled,
                        onCheckedChange = viewModel::onEnabledChange
                    )

                    Text(
                        if (config.hasKey) {
                            "API key: stored securely (••••••••) — type a new value to replace it"
                        } else {
                            "No API key set yet"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (config.hasKey) RoninSuccess else RoninWarningColor
                    )

                    RoninTextField(
                        value = config.apiKey,
                        onValueChange = viewModel::onApiKeyChange,
                        label = "API key",
                        placeholder = "Paste your API key here",
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    if (config.hasKey) {
                        GradientButton(
                            text = "Delete stored key",
                            onClick = viewModel::deleteKey,
                            icon = Icons.Rounded.DeleteOutline
                        )
                    }
                }
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MODEL", style = MaterialTheme.typography.labelLarge, color = RoninCyan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        config.type.suggestedModels().take(3).forEach { model ->
                            OptionChip(
                                label = model,
                                selected = config.model == model,
                                onClick = { viewModel.onModelChange(model) }
                            )
                        }
                    }
                    RoninTextField(
                        value = config.model,
                        onValueChange = viewModel::onModelChange,
                        label = "Model name",
                        placeholder = config.type.defaultModel,
                        singleLine = true
                    )
                }
            }

            if (config.type == AiProviderType.CUSTOM) {
                NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ENDPOINT", style = MaterialTheme.typography.labelLarge, color = RoninCyan)
                        RoninTextField(
                            value = config.baseUrl,
                            onValueChange = viewModel::onBaseUrlChange,
                            label = "Base URL",
                            placeholder = "https://your-endpoint.com/v1",
                            singleLine = true
                        )
                    }
                }
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TEMPERATURE", style = MaterialTheme.typography.labelLarge, color = RoninCyan)
                        Text(
                            String.format(java.util.Locale.US, "%.1f", config.temperature),
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoninTextSecondary
                        )
                    }
                    Slider(
                        value = config.temperature,
                        onValueChange = viewModel::onTemperatureChange,
                        valueRange = 0f..1.5f
                    )
                }
            }

            GradientButton(
                text = "Save provider",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

private val RoninWarningColor = androidx.compose.ui.graphics.Color(0xFFFBBF24)
