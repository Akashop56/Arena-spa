package com.ronin.ai.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ronin.ai.core.data.db.dao.ConversationDao
import com.ronin.ai.core.data.db.dao.ExperienceDao
import com.ronin.ai.core.data.db.dao.MemoryDao
import com.ronin.ai.core.data.db.dao.NotificationEventDao
import com.ronin.ai.core.data.db.dao.RoutineDao
import com.ronin.ai.core.data.db.dao.RoutineHistoryDao
import com.ronin.ai.core.data.db.entity.ConversationEntity
import com.ronin.ai.core.data.db.entity.ExperienceEntity
import com.ronin.ai.core.data.db.entity.MemoryEntity
import com.ronin.ai.core.data.db.entity.NotificationEventEntity
import com.ronin.ai.core.data.db.entity.RoutineEntity
import com.ronin.ai.core.data.db.entity.RoutineHistoryEntity

/**
 * RONIN's local brain storage: conversation, memory (short/long term,
 * preferences, learned solutions), experience log, routines + history and
 * notification events. Schema exports are disabled on purpose — the schema
 * is pinned to Room 2.6.1 and managed by migrations in future releases.
 */
@Database(
    entities = [
        MemoryEntity::class,
        ConversationEntity::class,
        ExperienceEntity::class,
        RoutineEntity::class,
        RoutineHistoryEntity::class,
        NotificationEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RoninDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun conversationDao(): ConversationDao
    abstract fun experienceDao(): ExperienceDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineHistoryDao(): RoutineHistoryDao
    abstract fun notificationEventDao(): NotificationEventDao
}
