package com.ronin.ai.core.domain.usecase

import com.ronin.ai.core.device.AppInfo
import com.ronin.ai.core.device.DeviceManager
import com.ronin.ai.core.domain.model.BatteryState
import com.ronin.ai.core.domain.model.DeviceInfo
import javax.inject.Inject
import javax.inject.Singleton

/** Device-control operations behind the UI layer. */
@Singleton
class DeviceUseCases @Inject constructor(
    private val deviceManager: DeviceManager
) {

    fun battery(): BatteryState = deviceManager.getBatteryState()

    fun deviceInfo(): DeviceInfo = deviceManager.getDeviceInfo()

    fun apps(): List<AppInfo> = deviceManager.getInstalledApps()

    fun launchApp(packageName: String): Boolean = deviceManager.launchApp(packageName)

    fun setVolumePercent(percent: Int): Boolean {
        val level = percent.coerceIn(0, 100) * deviceManager.maxVolume / 100
        return deviceManager.setVolume(level)
    }

    fun volumePercent(): Int =
        if (deviceManager.maxVolume <= 0) 0
        else deviceManager.getVolume() * 100 / deviceManager.maxVolume

    fun isTorchOn(): Boolean = deviceManager.isTorchOn()

    fun toggleTorch(on: Boolean): Boolean = deviceManager.toggleTorch(on)

    fun canWriteBrightness(): Boolean = deviceManager.canWriteBrightness()

    fun brightnessPercent(): Int = deviceManager.getBrightness() * 100 / 255

    fun setBrightnessPercent(percent: Int): Boolean =
        deviceManager.setBrightness(percent)

    fun openBatterySaverSettings() = deviceManager.openBatterySaverSettings()

    fun openWifiSettings() = deviceManager.openWifiSettings()

    fun openNotificationAccessSettings() = deviceManager.openNotificationAccessSettings()

    fun isNotificationAccessGranted(): Boolean = deviceManager.isNotificationAccessGranted()
}
