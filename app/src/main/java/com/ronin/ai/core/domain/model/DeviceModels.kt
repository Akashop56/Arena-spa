package com.ronin.ai.core.domain.model

/** Snapshot of battery state, read from the sticky battery broadcast. */
data class BatteryState(
    val level: Int,
    val isCharging: Boolean,
    val statusLabel: String,
    val temperatureC: Float,
    val healthLabel: String,
    val pluggedLabel: String
)

/** Static + dynamic hardware / system information. */
data class DeviceInfo(
    val brand: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val buildId: String,
    val cpuAbi: String,
    val storageTotalBytes: Long,
    val storageFreeBytes: Long,
    val ramTotalBytes: Long,
    val ramAvailableBytes: Long
) {
    val storageUsedPercent: Int
        get() = if (storageTotalBytes <= 0) 0 else ((storageTotalBytes - storageFreeBytes) * 100 / storageTotalBytes).toInt()
}

/** A notification event recorded by RONIN (own posts or, with access granted, other apps). */
data class NotificationEventItem(
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Long = System.currentTimeMillis(),
    val own: Boolean = true
)
