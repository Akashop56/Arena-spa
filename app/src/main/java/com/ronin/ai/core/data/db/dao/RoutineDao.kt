package com.ronin.ai.core.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ronin.ai.core.data.db.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT * FROM routines WHERE enabled = 1 AND triggerPhrase != ''")
    fun observeEnabledWithTrigger(): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RoutineEntity): Long

    @Update
    suspend fun update(entity: RoutineEntity)

    @Delete
    suspend fun delete(entity: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE routines SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM routines")
    suspend fun count(): Int
}
