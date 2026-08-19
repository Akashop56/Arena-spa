package com.ronin.ai.core.data.repository

import com.ronin.ai.core.data.db.dao.NotificationEventDao
import com.ronin.ai.core.data.db.entity.toDomain
import com.ronin.ai.core.data.db.entity.toEntity
import com.ronin.ai.core.domain.model.NotificationEventItem
import com.ronin.ai.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationEventDao
) : NotificationRepository {

    override fun events(): Flow<List<NotificationEventItem>> =
        dao.observeRecent(100).map { list -> list.map { it.toDomain() } }

    override suspend fun record(event: NotificationEventItem) {
        dao.insert(event.toEntity())
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
