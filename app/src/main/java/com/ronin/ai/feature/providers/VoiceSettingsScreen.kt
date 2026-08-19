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
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.VolumeUp
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
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.OptionChip
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.RoninTextField
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.domain.model.VoiceLanguage
import com.ronin.ai.core.domain.model.VoiceProviderType
import com.ronin.ai.core.domain.model.VoiceSettings

@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    viewModel: VoiceSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

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
            RoninHeader(title = "Voice", subtitle = "settings", onBack = onBack)

            // Provider selection
            Text("PROVIDER", style = MaterialTheme.typography.labelLarge, color = RoninCyan)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceProviderType.entries.forEach { provider ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OptionChip(
                            label = provider.displayName,
                            selected = settings.provider == provider,
                            onClick = { viewModel.onProviderChange(provider) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (settings.provider != VoiceProviderType.SYSTEM) {
                NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (settings.hasKey) "API key: stored securely (••••••••)" else "API key required",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (settings.hasKey) RoninSuccess else RoninWarningColor
                        )
                        RoninTextField(
                            value = settings.apiKey,
                            onValueChange = viewModel::onApiKeyChange,
                            label = "API key",
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        when (settings.provider) {
                            VoiceProviderType.ELEVENLABS -> {
                                RoninTextField(
                                    value = settings.voiceId,
                                    onValueChange = viewModel::onVoiceIdChange,
                                    label = "Voice ID",
                                    placeholder = "e.g. 21m00Tcm4TlvDq8ikWAM",
                                    singleLine = true
                                )
                                RoninTextField(
                                    value = settings.model,
                                    onValueChange = viewModel::onModelChange,
                                    label = "Model",
                                    placeholder = "eleven_multilingual_v2",
                                    singleLine = true
                                )
                            }
                            VoiceProviderType.GOOGLE_CLOUD -> {
                                RoninTextField(
                                    value = settings.voiceId,
                                    onValueChange = viewModel::onVoiceIdChange,
                                    label = "Voice name",
                                    placeholder = "en-US-Neural2-F / hi-IN-Standard-A",
                                    singleLine = true
                                )
                            }
                            VoiceProviderType.AZURE -> {
                                RoninTextField(
                                    value = settings.voiceId,
                                    onValueChange = viewModel::onVoiceIdChange,
                                    label = "Voice name",
                                    placeholder = "en-US-AriaNeural / hi-IN-MadhurNeural",
                                    singleLine = true
                                )
                                RoninTextField(
                                    value = settings.model,
                                    onValueChange = viewModel::onModelChange,
                                    label = "Region",
                                    placeholder = "eastus",
                                    singleLine = true
                                )
                            }
                            VoiceProviderType.CUSTOM -> {
                                RoninTextField(
                                    value = settings.endpoint,
                                    onValueChange = viewModel::onEndpointChange,
                                    label = "Base URL",
                                    placeholder = "https://your-endpoint.com/v1",
                                    singleLine = true
                                )
                                RoninTextField(
                                    value = settings.model,
                                    onValueChange = viewModel::onModelChange,
                                    label = "Model",
                                    placeholder = "tts-1",
                                    singleLine = true
                                )
                                RoninTextField(
                                    value = settings.voiceId,
                                    onValueChange = viewModel::onVoiceIdChange,
                                    label = "Voice",
                                    placeholder = "alloy",
                                    singleLine = true
                                )
                            }
                            VoiceProviderType.SYSTEM -> Unit
                        }
                    }
                }
            }

            // Language
            Text("LANGUAGE", style = MaterialTheme.typography.labelLarge, color = RoninCyan)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceLanguage.entries.forEach { lang ->
                    OptionChip(
                        label = lang.label,
                        selected = settings.language == lang.code,
                        onClick = { viewModel.onLanguageChange(lang.code) }
                    )
                }
            }

            // Speed + pitch
            NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SPEED", style = MaterialTheme.typography.labelLarge, color = RoninCyan)
                        Text(
                            String.format(java.util.Locale.US, "%.1fx", settings.speed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoninTextSecondary
                        )
                    }
                    Slider(
                        value = settings.speed,
                        onValueChange = viewModel::onSpeedChange,
                        valueRange = 0.5f..2f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PITCH", style = MaterialTheme.typography.labelLarge, color = RoninCyan)
                        Text(
                            String.format(java.util.Locale.US, "%.1fx", settings.pitch),
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoninTextSecondary
                        )
                    }
                    Slider(
                        value = settings.pitch,
                        onValueChange = viewModel::onPitchChange,
                        valueRange = 0.5f..2f
                    )
                }
            }

            GradientButton(
                text = "Test voice",
                onClick = viewModel::testVoice,
                icon = Icons.Rounded.VolumeUp,
                modifier = Modifier.fillMaxWidth()
            )

            GradientButton(
                text = "Save voice settings",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Key,
                    contentDescription = null,
                    tint = RoninTextSecondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "Hindi (hi-IN) and English (en-US) are supported. If a cloud voice fails, RONIN automatically falls back to the offline system voice.",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoninTextSecondary
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

private val RoninWarningColor = androidx.compose.ui.graphics.Color(0xFFFBBF24)
