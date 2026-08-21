package com.ronin.ai.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.design.components.ErrorBanner
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.OptionChip
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.StatusChip
import com.ronin.ai.core.design.components.WaveformBars
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninAmber

@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val partialText by viewModel.partialText.collectAsStateWithLifecycle()
    val lastTranscript by viewModel.lastTranscript.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val providerLabel by viewModel.providerLabel.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) viewModel.startListening()
    }

    RoninBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoninHeader(title = "Voice", subtitle = providerLabel)

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip(
                    label = "English",
                    selected = language.startsWith("en"),
                    onClick = { if (!language.startsWith("en")) viewModel.toggleLanguage() }
                )
                OptionChip(
                    label = "हिन्दी",
                    selected = language.startsWith("hi"),
                    onClick = { if (!language.startsWith("hi")) viewModel.toggleLanguage() }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Mic orb
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) RoninCyan.copy(alpha = 0.16f)
                        else RoninAmber.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WaveformBars(
                        active = isListening,
                        color = if (isListening) RoninCyan else RoninAmber
                    )
                    Spacer(Modifier.height(10.dp))
                    Icon(
                        if (isListening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = if (isListening) RoninCyan else RoninAmber,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            GradientButton(
                text = when {
                    isListening -> "Stop listening"
                    isThinking -> "Thinking…"
                    else -> "Tap to speak"
                },
                onClick = {
                    if (isListening) {
                        viewModel.stopListening()
                    } else if (!isThinking) {
                        if (hasMicPermission) viewModel.startListening()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !isThinking,
                icon = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic
            )

            Spacer(Modifier.height(16.dp))

            if (error != null) {
                ErrorBanner(error ?: "", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }

            if (partialText.isNotBlank()) {
                Text(
                    partialText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = RoninCyan,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            }

            if (lastTranscript.isNotBlank()) {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.RecordVoiceOver,
                                contentDescription = null,
                                tint = RoninCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "YOU SAID",
                                style = MaterialTheme.typography.labelLarge,
                                color = RoninCyan,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            if (isSpeaking) StatusChip("SPEAKING", RoninSuccess)
                            else if (isThinking) StatusChip("THINKING", RoninCyan)
                        }
                        Text(
                            lastTranscript,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            GradientButton(
                text = "Test voice",
                onClick = viewModel::testVoice,
                icon = Icons.Rounded.VolumeUp,
                enabled = !isThinking && !isListening
            )

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Language,
                    contentDescription = null,
                    tint = RoninTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "  Offline fallback enabled — if the cloud voice fails, RONIN switches to the device voice automatically.",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoninTextSecondary
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
