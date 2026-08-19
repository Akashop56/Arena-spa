package com.ronin.ai.core.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ronin.ai.core.data.db.entity.ExperienceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperienceDao {

    @Query("SELECT * FROM experiences ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExperienceEntity>>

    @Query("SELECT * FROM experiences WHERE category = :category ORDER BY createdAt DESC")
    fun observeByCategory(category: String): Flow<List<ExperienceEntity>>

    @Query("SELECT * FROM experiences WHERE resolved = 0 ORDER BY createdAt DESC")
    fun observeUnresolved(): Flow<List<ExperienceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExperienceEntity): Long

    @Delete
    suspend fun delete(entity: ExperienceEntity)

    @Query("DELETE FROM experiences WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE experiences SET resolved = 1 WHERE id = :id")
    suspend fun markResolved(id: Long)

    @Query("DELETE FROM experiences")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM experiences")
    suspend fun count(): Int
}
