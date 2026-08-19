package com.ronin.ai.feature.device

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ronin.ai.core.common.TimeFormat
import com.ronin.ai.core.design.components.EmptyState
import com.ronin.ai.core.design.components.GradientButton
import com.ronin.ai.core.design.components.KeyValueRow
import com.ronin.ai.core.design.components.NeonCard
import com.ronin.ai.core.design.components.RoninBackground
import com.ronin.ai.core.design.components.RoninHeader
import com.ronin.ai.core.design.components.RoninTextField
import com.ronin.ai.core.design.components.SectionHeader
import com.ronin.ai.core.design.components.StatusChip
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninError
import com.ronin.ai.core.design.theme.RoninSuccess
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.core.design.theme.RoninViolet
import com.ronin.ai.core.design.theme.RoninWarning
import com.ronin.ai.core.domain.model.NotificationEventItem

@Composable
fun DeviceScreen(viewModel: DeviceViewModel = hiltViewModel()) {
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
    val torchOn by viewModel.torchOn.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val canWriteBrightness by viewModel.canWriteBrightness.collectAsStateWithLifecycle()
    val notifAccess by viewModel.notifAccess.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val appQuery by viewModel.appQuery.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) viewModel.toggleTorch(!torchOn)
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
        viewModel.loadApps()
    }

    RoninBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { RoninHeader(title = "Device") }

            item {
                SectionHeader(title = "Battery")
            }
            item {
                battery?.let {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (it.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                                    contentDescription = null,
                                    tint = if (it.level <= 20) RoninWarning else RoninCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    "${it.level}%",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                                Spacer(Modifier.weight(1f))
                                StatusChip(
                                    if (it.isCharging) "CHARGING" else it.statusLabel.uppercase(),
                                    if (it.isCharging) RoninSuccess else RoninCyan
                                )
                            }
                            LinearProgressIndicator(
                                progress = { it.level / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = if (it.level <= 20) RoninWarning else RoninCyan,
                                trackColor = Color(0xFF1A2338)
                            )
                            KeyValueRow("Temperature", "${it.temperatureC}°C")
                            KeyValueRow("Power source", it.pluggedLabel)
                            KeyValueRow("Health", it.healthLabel)
                        }
                    }
                }
            }

            item { SectionHeader(title = "Quick controls") }
            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ControlRow(
                            icon = Icons.Rounded.FlashlightOn,
                            title = "Torch",
                            subtitle = if (torchOn) "On" else "Off",
                            action = {
                                if (hasCameraPermission) viewModel.toggleTorch(!torchOn)
                                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                        ControlRow(
                            icon = Icons.Rounded.VolumeUp,
                            title = "Volume",
                            subtitle = "$volume%"
                        ) {
                            Slider(
                                value = volume.toFloat(),
                                onValueChange = { viewModel.setVolume(it.toInt()) },
                                valueRange = 0f..100f
                            )
                        }
                        ControlRow(
                            icon = Icons.Rounded.BrightnessHigh,
                            title = "Brightness",
                            subtitle = if (canWriteBrightness) "$brightness%" else "Permission needed"
                        ) {
                            if (canWriteBrightness) {
                                Slider(
                                    value = brightness.toFloat(),
                                    onValueChange = { viewModel.setBrightness(it.toInt()) },
                                    valueRange = 0f..100f
                                )
                            } else {
                                GradientButton(
                                    text = "Grant permission",
                                    onClick = {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                android.net.Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientButton(
                                text = "Battery saver",
                                onClick = viewModel::openBatterySaverSettings,
                                icon = Icons.Rounded.EnergySavingsLeaf,
                                modifier = Modifier.weight(1f)
                            )
                            GradientButton(
                                text = "Wi-Fi",
                                onClick = viewModel::openWifiSettings,
                                icon = Icons.Rounded.Wifi,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item { SectionHeader(title = "Device information") }
            item {
                deviceInfo?.let { info ->
                    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            KeyValueRow("Model", "${info.brand} ${info.model}")
                            KeyValueRow("Android", "${info.androidVersion} (API ${info.sdkInt})")
                            KeyValueRow("CPU", info.cpuAbi)
                            KeyValueRow(
                                "Storage",
                                "${info.storageUsedPercent}% used · ${formatBytes(info.storageFreeBytes)} free"
                            )
                            KeyValueRow("RAM", formatBytes(info.ramTotalBytes))
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Notifications",
                    trailing = {
                        if (notifications.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearNotifications) {
                                Icon(
                                    Icons.Rounded.DeleteSweep,
                                    contentDescription = "Clear",
                                    tint = RoninError
                                )
                            }
                        }
                    }
                )
            }
            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KeyValueRow(
                            "Notification access",
                            if (notifAccess) "Granted" else "Not granted"
                        )
                        if (!notifAccess) {
                            GradientButton(
                                text = "Enable in system settings",
                                onClick = viewModel::openNotificationAccessSettings
                            )
                        }
                    }
                }
            }
            if (notifications.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Notifications,
                        title = "No notification history",
                        subtitle = "RONIN's own notifications and (with access granted) other apps' notifications appear here."
                    )
                }
            } else {
                items(notifications.take(15), key = { it.id }) { event ->
                    NotificationRow(event)
                }
            }

            item { SectionHeader(title = "Open app", modifier = Modifier.padding(top = 8.dp)) }
            item {
                RoninTextField(
                    value = appQuery,
                    onValueChange = viewModel::onAppQueryChange,
                    placeholder = "Search installed apps…",
                    leadingIcon = Icons.Rounded.Search,
                    singleLine = true
                )
            }
            val filtered = if (appQuery.isBlank()) apps.take(40) else apps.filter {
                it.label.contains(appQuery, ignoreCase = true) || it.packageName.contains(appQuery, ignoreCase = true)
            }.take(20)
            items(filtered, key = { it.packageName }) { app ->
                AppRow(app) { viewModel.launchApp(app.packageName) }
            }
        }
    }
}

@Composable
private fun ControlRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = RoninCyan, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = RoninTextSecondary)
        }
        action?.invoke()
    }
}

@Composable
private fun NotificationRow(event: NotificationEventItem) {
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    event.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    if (event.own) "RONIN" else "APP",
                    if (event.own) RoninCyan else RoninViolet
                )
            }
            if (event.title.isNotBlank()) {
                Text(event.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (event.text.isNotBlank()) {
                Text(event.text, style = MaterialTheme.typography.bodyMedium, color = RoninTextSecondary)
            }
            Text(
                TimeFormat.relative(event.postedAt),
                style = MaterialTheme.typography.labelMedium,
                color = RoninTextSecondary
            )
        }
    }
}

@Composable
private fun AppRow(app: com.ronin.ai.core.device.AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(app.icon) {
            app.icon?.toBitmap(48, 48)?.asImageBitmap()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = RoninTextSecondary,
                modifier = Modifier.size(36.dp)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(app.packageName, style = MaterialTheme.typography.labelMedium, color = RoninTextSecondary)
        }
        Icon(Icons.Rounded.Settings, contentDescription = null, tint = RoninTextSecondary, modifier = Modifier.size(16.dp))
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1) String.format(java.util.Locale.US, "%.1f GB", gb)
    else String.format(java.util.Locale.US, "%.0f MB", bytes / (1024.0 * 1024.0))
}
