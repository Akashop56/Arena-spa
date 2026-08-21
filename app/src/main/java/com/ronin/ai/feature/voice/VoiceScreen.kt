package com.ronin.ai.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.MicOff
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.design.components.ErrorBanner
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.MarkdownText
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.OptionChip
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.StatusChip
import com.ronin.ai.core.design.components.WaveformBars
import com.ronin.ai.core.design.theme.RoninAmber
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary

@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val micState by viewModel.micState.collectAsStateWithLifecycle()
    val micLevel by viewModel.micLevel.collectAsStateWithLifecycle()
    val partialText by viewModel.partialText.collectAsStateWithLifecycle()
    val lastTranscript by viewModel.lastTranscript.collectAsStateWithLifecycle()
    val lastReply by viewModel.lastReply.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val providerLabel by viewModel.providerLabel.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val listening = micState == MicState.LISTENING
    val processing = micState == MicState.PROCESSING
    val speaking = micState == MicState.SPEAKING
    val unavailable = micState == MicState.UNAVAILABLE
    val busy = processing || speaking

    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        permissionDenied = !granted
        if (granted) viewModel.startListening()
    }

    // The orb breathes with the measured input level while listening.
    val orbScale by animateFloatAsState(
        targetValue = if (listening) 1f + (micLevel * 0.22f) else 1f,
        label = "orbScale"
    )

    RoninBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoninHeader(title = "Voice", subtitle = providerLabel)

            Spacer(Modifier.height(12.dp))

            // ---- Explicit microphone state indicator ----
            when {
                unavailable -> StatusChip("MIC UNAVAILABLE", RoninError)
                listening -> StatusChip("LISTENING", RoninCyan)
                processing -> StatusChip("THINKING", RoninAmber)
                speaking -> StatusChip("SPEAKING", RoninSuccess)
                !hasMicPermission -> StatusChip("MIC PERMISSION NEEDED", RoninAmber)
                else -> StatusChip("READY", RoninTextSecondary)
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip(
                    label = "English",
                    selected = language.startsWith("en"),
                    onClick = { viewModel.setLanguage("en-US") }
                )
                OptionChip(
                    label = "हिन्दी",
                    selected = language.startsWith("hi"),
                    onClick = { viewModel.setLanguage("hi-IN") }
                )
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(orbScale)
                    .clip(CircleShape)
                    .background(
                        when {
                            listening -> RoninCyan.copy(alpha = 0.16f)
                            speaking -> RoninSuccess.copy(alpha = 0.12f)
                            processing -> RoninAmber.copy(alpha = 0.12f)
                            else -> RoninCyan.copy(alpha = 0.06f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WaveformBars(
                        active = listening || speaking,
                        color = if (speaking) RoninSuccess else RoninCyan
                    )
                    Spacer(Modifier.height(10.dp))
                    Icon(
                        when {
                            unavailable || !hasMicPermission -> Icons.Rounded.MicOff
                            listening -> Icons.Rounded.GraphicEq
                            else -> Icons.Rounded.Mic
                        },
                        contentDescription = null,
                        tint = when {
                            unavailable || !hasMicPermission -> RoninError
                            listening -> RoninCyan
                            speaking -> RoninSuccess
                            else -> RoninAmber
                        },
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            GradientButton(
                text = when {
                    unavailable -> "Unavailable"
                    listening -> "Stop listening"
                    processing -> "Thinking…"
                    speaking -> "Stop speaking"
                    else -> "Tap to speak"
                },
                onClick = {
                    when {
                        unavailable -> Unit
                        listening -> viewModel.stopListening()
                        busy -> viewModel.stopAll()
                        hasMicPermission -> viewModel.startListening()
                        else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !unavailable,
                icon = if (listening || busy) Icons.Rounded.Stop else Icons.Rounded.Mic
            )

            Spacer(Modifier.height(16.dp))

            if (permissionDenied && !hasMicPermission) {
                ErrorBanner(
                    "Microphone access was denied. Enable it in system settings to talk to RONIN.",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

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
                        }
                        Text(
                            lastTranscript,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (lastReply.isNotBlank()) {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = RoninAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "RONIN",
                                style = MaterialTheme.typography.labelLarge,
                                color = RoninAmber,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        MarkdownText(text = lastReply)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            GradientButton(
                text = "Test voice",
                onClick = viewModel::testVoice,
                icon = Icons.Rounded.VolumeUp,
                enabled = !listening && !processing
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
