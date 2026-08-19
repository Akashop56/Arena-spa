package com.ronin.ai.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ronin.ai.core.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ConversationEntity>

    @Insert
    suspend fun insert(entity: ConversationEntity): Long

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("DELETE FROM conversations WHERE id NOT IN (SELECT id FROM conversations ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int
}
