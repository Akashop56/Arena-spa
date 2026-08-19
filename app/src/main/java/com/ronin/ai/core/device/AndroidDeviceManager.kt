package com.ronin.ai.core.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.app.NotificationManager
import android.provider.Settings.Secure
import com.ronin.ai.core.domain.model.BatteryState
import com.ronin.ai.core.domain.model.DeviceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of the device layer. All Android framework
 * calls are contained here — nothing above this class touches the framework
 * for device control.
 */
@Singleton
class AndroidDeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceManager {

    private val packageManager = context.packageManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // ---------------------------------------------------------------- battery
    override fun getBatteryState(): BatteryState {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        val statusLabel = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
        val pluggedLabel = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }
        val healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            else -> "Unknown"
        }
        return BatteryState(
            level = if (level < 0) 0 else (level * 100 / scale).coerceIn(0, 100),
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            statusLabel = statusLabel,
            temperatureC = temp,
            healthLabel = healthLabel,
            pluggedLabel = pluggedLabel
        )
    }

    // ------------------------------------------------------------- device info
    override fun getDeviceInfo(): DeviceInfo {
        val stats = StatFs(Environment.getDataDirectory().absolutePath)
        val storageTotal = stats.blockCountLong * stats.blockSizeLong
        val storageFree = stats.availableBlocksLong * stats.blockSizeLong

        val memInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memInfo)

        return DeviceInfo(
            brand = Build.BRAND,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            buildId = Build.DISPLAY,
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            storageTotalBytes = storageTotal,
            storageFreeBytes = storageFree,
            ramTotalBytes = memInfo.totalMem,
            ramAvailableBytes = memInfo.availMem
        )
    }

    // ------------------------------------------------------------------- apps
    override fun getInstalledApps(): List<AppInfo> = runCatching {
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launchIntent, 0)
            .map { resolveInfo ->
                val label = resolveInfo.loadLabel(packageManager)?.toString() ?: resolveInfo.activityInfo.packageName
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = label,
                    icon = runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())

    override fun launchApp(packageName: String): Boolean = runCatching {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    override fun openUrl(url: String): Boolean = runCatching {
        val uri = if (url.startsWith("http://") || url.startsWith("https://")) {
            Uri.parse(url)
        } else {
            Uri.parse("https://$url")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    override fun openAppDetails(packageName: String): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    // ------------------------------------------------------------------- audio
    override fun getVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    override val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    override fun setVolume(level: Int): Boolean = runCatching {
        val clamped = level.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        true
    }.getOrDefault(false)

    override fun adjustVolume(delta: Int): Boolean = runCatching {
        val flags = if (delta > 0) AudioManager.FLAG_SHOW_UI else 0
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            flags
        )
        true
    }.getOrDefault(false)

    // ------------------------------------------------------------------- torch
    private var torchState = false

    private fun torchCameraId(): String? = runCatching {
        cameraManager.cameraIdList.firstOrNull { id: String ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    override fun isTorchOn(): Boolean = torchState

    override fun toggleTorch(on: Boolean): Boolean = runCatching {
        val id = torchCameraId() ?: return false
        cameraManager.setTorchMode(id, on)
        torchState = on
        true
    }.getOrDefault(false)

    // -------------------------------------------------------------- brightness
    override fun canWriteBrightness(): Boolean = Settings.System.canWrite(context)

    override fun getBrightness(): Int = runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
    }.getOrDefault(128)

    override fun setBrightness(percent: Int): Boolean {
        if (!canWriteBrightness()) return false
        return runCatching {
            val value = (percent.coerceIn(0, 100) * 255 / 100)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value
            )
            true
        }.getOrDefault(false)
    }

    // ---------------------------------------------------- settings shortcuts
    override fun openBatterySaverSettings(): Boolean = openSettings(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))

    override fun openWifiSettings(): Boolean = openSettings(Intent(Settings.ACTION_WIFI_SETTINGS))

    override fun openNotificationAccessSettings(): Boolean =
        openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    override fun isNotificationAccessGranted(): Boolean {
        val enabled = Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabled.split(":").any { it.contains(context.packageName) }
    }

    private fun openSettings(intent: Intent): Boolean = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
