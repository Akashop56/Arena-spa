package com.ronin.ai.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ronin.ai.core.data.db.entity.RoutineHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineHistoryDao {

    @Query("SELECT * FROM routine_history ORDER BY executedAt DESC")
    fun observeAll(): Flow<List<RoutineHistoryEntity>>

    @Query("SELECT * FROM routine_history WHERE routineId = :routineId ORDER BY executedAt DESC")
    fun observeForRoutine(routineId: Long): Flow<List<RoutineHistoryEntity>>

    @Insert
    suspend fun insert(entity: RoutineHistoryEntity): Long

    @Query("DELETE FROM routine_history WHERE routineId = :routineId")
    suspend fun deleteForRoutine(routineId: Long)

    @Query("DELETE FROM routine_history")
    suspend fun clearAll()
}
