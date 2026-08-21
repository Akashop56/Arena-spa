package com.ronin.ai.core.device

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ronin.ai.MainActivity
import com.ronin.ai.R
import com.ronin.ai.core.common.Constants
import com.ronin.ai.core.domain.model.NotificationEventItem
import com.ronin.ai.core.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts RONIN's own notifications and records every posted event into the
 * notification memory so the assistant can answer "what did you notify me?".
 */
@Singleton
class NotificationCenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository
) {

    // Resolved lazily and defensively: this class is a @Singleton in the Hilt
    // graph, so throwing in the constructor (hard cast, or createNotification-
    // Channel on a locked/restricted device) would take down app startup.
    private val notificationManager: NotificationManager? by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var channelReady = false

    /** Creates the notification channel on first use. Idempotent. */
    private fun ensureChannel(): NotificationManager? {
        val manager = notificationManager ?: return null
        if (channelReady) return manager
        runCatching {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            manager.createNotificationChannel(channel)
            channelReady = true
        }
        return manager
    }

    fun hasPostPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    /** Posts a notification, optionally after [delayMillis]. */
    fun post(
        title: String,
        text: String,
        delayMillis: Long = 0L,
        id: Int = System.currentTimeMillis().toInt()
    ) {
        scope.launch {
            if (delayMillis > 0) delay(delayMillis)
            if (!hasPostPermission()) return@launch
            postInternal(title, text, id)
            notificationRepository.record(
                NotificationEventItem(
                    packageName = context.packageName,
                    appName = Constants.NOTIFICATION_SOURCE,
                    title = title,
                    text = text,
                    postedAt = System.currentTimeMillis(),
                    own = true
                )
            )
        }
    }

    private fun postInternal(title: String, text: String, id: Int) {
        // Channel is created on first post rather than at injection time.
        val manager = ensureChannel() ?: return
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        // notify() throws if POST_NOTIFICATIONS was revoked between the check
        // and the call, so never let a reminder crash the app.
        runCatching { manager.notify(id, notification) }
    }
}
