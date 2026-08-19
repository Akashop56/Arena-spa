package com.ronin.ai.core.domain.repository

import com.ronin.ai.core.domain.model.NotificationEventItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun events(): Flow<List<NotificationEventItem>>
    suspend fun record(event: NotificationEventItem)
    suspend fun clearAll()
}
