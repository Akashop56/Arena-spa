package com.ronin.ai.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ronin.ai.core.data.db.entity.NotificationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {

    @Query("SELECT * FROM notification_events ORDER BY postedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NotificationEventEntity>>

    @Insert
    suspend fun insert(entity: NotificationEventEntity): Long

    @Query("DELETE FROM notification_events")
    suspend fun clearAll()
}
