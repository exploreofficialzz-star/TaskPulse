package com.chastechgroup.taskpulse.data.db.dao

import androidx.room.*
import com.chastechgroup.taskpulse.data.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("UPDATE sessions SET endTime = :endTime, isActive = 0 WHERE id = :id")
    suspend fun endSession(id: Long, endTime: Long)

    @Query("SELECT * FROM sessions WHERE ruleId = :ruleId AND isActive = 1")
    fun getActiveSessions(ruleId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isActive = 1")
    suspend fun getAllActiveSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 50")
    fun getRecentSessions(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE startTime < :cutoff")
    suspend fun deleteOldSessions(cutoff: Long)
}
