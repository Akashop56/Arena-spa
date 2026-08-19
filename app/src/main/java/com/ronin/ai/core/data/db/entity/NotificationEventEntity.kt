package com.ronin.ai.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ronin.ai.core.domain.model.NotificationEventItem

@Entity(
    tableName = "notification_events",
    indices = [Index("postedAt")]
)
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val own: Boolean
)

fun NotificationEventEntity.toDomain(): NotificationEventItem = NotificationEventItem(
    id = id,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    postedAt = postedAt,
    own = own
)

fun NotificationEventItem.toEntity(): NotificationEventEntity = NotificationEventEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    postedAt = postedAt,
    own = own
)
