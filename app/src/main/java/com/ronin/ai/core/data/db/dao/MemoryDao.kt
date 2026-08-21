package com.ronin.ai.core.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ronin.ai.core.data.db.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE type = :type ORDER BY updatedAt DESC")
    fun observeByType(type: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MemoryEntity>>

    @Query(
        "SELECT * FROM memories WHERE title LIKE '%' || :query || '%' " +
            "OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' " +
            "ORDER BY importance DESC, updatedAt DESC"
    )
    fun observeSearch(query: String): Flow<List<MemoryEntity>>

    @Query(
        "SELECT * FROM memories WHERE (title LIKE '%' || :query || '%' " +
            "OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') " +
            "AND type != 'CONVERSATION' ORDER BY importance DESC, updatedAt DESC LIMIT :limit"
    )
    fun observeSearchNonConversation(query: String, limit: Int): Flow<List<MemoryEntity>>

    @Query(
        "SELECT * FROM memories WHERE (title LIKE '%' || :query || '%' " +
            "OR content LIKE '%' || :query || '%') AND type != 'CONVERSATION' " +
            "ORDER BY importance DESC, updatedAt DESC LIMIT :limit"
    )
    suspend fun searchNonConversation(query: String, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE type = :type AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun searchByType(type: String, query: String, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: Long): MemoryEntity?

    /** Exact duplicate probe used before auto-saving an extracted preference. */
    @Query(
        "SELECT COUNT(*) FROM memories WHERE type = :type AND title = :title AND content = :content"
    )
    suspend fun countMatching(type: String, title: String, content: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MemoryEntity): Long

    @Update
    suspend fun update(entity: MemoryEntity)

    @Delete
    suspend fun delete(entity: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memories WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM memories")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM memories WHERE type = :type")
    suspend fun countByType(type: String): Int
}
