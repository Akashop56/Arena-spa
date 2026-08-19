package com.ronin.ai.feature.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ronin.ai.core.device.AppInfo
import com.ronin.ai.core.domain.model.BatteryState
import com.ronin.ai.core.domain.model.DeviceInfo
import com.ronin.ai.core.domain.model.NotificationEventItem
import com.ronin.ai.core.domain.repository.NotificationRepository
import com.ronin.ai.core.domain.usecase.DeviceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceUseCases: DeviceUseCases,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _battery = MutableStateFlow<BatteryState?>(null)
    val battery: StateFlow<BatteryState?> = _battery.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _torchOn = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn.asStateFlow()

    private val _volume = MutableStateFlow(50)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _brightness = MutableStateFlow(50)
    val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _canWriteBrightness = MutableStateFlow(false)
    val canWriteBrightness: StateFlow<Boolean> = _canWriteBrightness.asStateFlow()

    private val _notifAccess = MutableStateFlow(false)
    val notifAccess: StateFlow<Boolean> = _notifAccess.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _appQuery = MutableStateFlow("")
    val appQuery: StateFlow<String> = _appQuery.asStateFlow()

    val notifications: StateFlow<List<NotificationEventItem>> = notificationRepository.events()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        _battery.value = runCatching { deviceUseCases.battery() }.getOrNull()
        _deviceInfo.value = runCatching { deviceUseCases.deviceInfo() }.getOrNull()
        _torchOn.value = runCatching { deviceUseCases.isTorchOn() }.getOrDefault(false)
        _volume.value = runCatching { deviceUseCases.volumePercent() }.getOrDefault(50)
        _brightness.value = runCatching { deviceUseCases.brightnessPercent() }.getOrDefault(50)
        _canWriteBrightness.value = deviceUseCases.canWriteBrightness()
        _notifAccess.value = deviceUseCases.isNotificationAccessGranted()
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.Default) {
            _apps.value = runCatching { deviceUseCases.apps() }.getOrDefault(emptyList())
        }
    }

    fun onAppQueryChange(value: String) {
        _appQuery.value = value
    }

    fun toggleTorch(on: Boolean): Boolean {
        val ok = deviceUseCases.toggleTorch(on)
        _torchOn.value = deviceUseCases.isTorchOn()
        return ok
    }

    fun setVolume(percent: Int) {
        deviceUseCases.setVolumePercent(percent)
        _volume.value = deviceUseCases.volumePercent()
    }

    fun setBrightness(percent: Int) {
        if (deviceUseCases.setBrightnessPercent(percent)) {
            _brightness.value = percent
        }
    }

    fun openBatterySaverSettings() = deviceUseCases.openBatterySaverSettings()

    fun openWifiSettings() = deviceUseCases.openWifiSettings()

    fun openNotificationAccessSettings() = deviceUseCases.openNotificationAccessSettings()

    fun clearNotifications() {
        viewModelScope.launch { notificationRepository.clearAll() }
    }

    fun launchApp(packageName: String) {
        deviceUseCases.launchApp(packageName)
    }
}
