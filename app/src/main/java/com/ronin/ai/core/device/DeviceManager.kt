package com.ronin.ai.core.device

import android.graphics.drawable.Drawable
import com.ronin.ai.core.domain.model.BatteryState
import com.ronin.ai.core.domain.model.DeviceInfo

/**
 * Contract for everything RONIN can do with the Android device. The UI and
 * AI layers depend only on this interface — the implementation lives in the
 * device layer and stays fully separated from Compose.
 */
interface DeviceManager {

    fun getBatteryState(): BatteryState

    fun getDeviceInfo(): DeviceInfo

    fun getInstalledApps(): List<AppInfo>

    fun launchApp(packageName: String): Boolean

    fun openUrl(url: String): Boolean

    fun openAppDetails(packageName: String): Boolean

    // ---- audio ----
    fun getVolume(): Int
    fun setVolume(level: Int): Boolean
    fun adjustVolume(delta: Int): Boolean
    val maxVolume: Int

    // ---- torch ----
    fun isTorchOn(): Boolean
    fun toggleTorch(on: Boolean): Boolean

    // ---- brightness (needs WRITE_SETTINGS) ----
    fun canWriteBrightness(): Boolean
    fun getBrightness(): Int
    fun setBrightness(percent: Int): Boolean

    // ---- system settings shortcuts ----
    fun openBatterySaverSettings(): Boolean
    fun openWifiSettings(): Boolean
    fun openNotificationAccessSettings(): Boolean
    fun isNotificationAccessGranted(): Boolean
}

/** Installed app entry. The icon is a Drawable so screens can render it directly. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)
