package com.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymbuddy.data.local.SessionWithSets
import com.gymbuddy.data.local.entities.SetLogEntity
import com.gymbuddy.data.local.entities.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSessionWithSets(id: Long): Flow<SessionWithSets?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionWithSets>>

    @Query("SELECT startedAt FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeCompletedTimestamps(): Flow<List<Long>>

    @Query("SELECT startedAt FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    suspend fun completedTimestampsNow(): List<Long>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    suspend fun setsForSession(sessionId: Long): List<SetLogEntity>

    @Query("SELECT COUNT(*) FROM sessions WHERE endedAt IS NOT NULL")
    fun observeCompletedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetLogEntity): Long

    @Update
    suspend fun updateSet(set: SetLogEntity)

    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    fun observeSets(sessionId: Long): Flow<List<SetLogEntity>>

    /** Most recent completed sets for an exercise, for progression suggestions. */
    @Query(
        """
        SELECT * FROM set_logs
        WHERE exerciseId = :exerciseId AND done = 1
        ORDER BY loggedAt DESC LIMIT :limit
        """,
    )
    suspend fun recentSetsFor(exerciseId: String, limit: Int): List<SetLogEntity>
}
