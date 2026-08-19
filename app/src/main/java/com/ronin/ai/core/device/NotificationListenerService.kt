package com.ronin.ai.core.device

import android.service.notification.NotificationListenerService as SystemNotificationListenerService
import android.service.notification.StatusBarNotification
import com.ronin.ai.core.domain.model.NotificationEventItem
import com.ronin.ai.core.domain.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Records notifications posted by other apps into RONIN's notification
 * memory. Requires the user to grant "Notification access" in system
 * settings (offered in Device Control → Notifications).
 */
@AndroidEntryPoint
class NotificationListenerService : SystemNotificationListenerService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        runCatching {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
            if (title.isBlank() && text.isBlank()) return
            serviceScope.launch {
                notificationRepository.record(
                    NotificationEventItem(
                        packageName = sbn.packageName,
                        appName = runCatching {
                            packageManager.getApplicationLabel(
                                packageManager.getApplicationInfo(sbn.packageName, 0)
                            ).toString()
                        }.getOrDefault(sbn.packageName),
                        title = title,
                        text = text,
                        postedAt = System.currentTimeMillis(),
                        own = false
                    )
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
