package com.chastechgroup.taskpulse.data.db.dao

import androidx.room.*
import com.chastechgroup.taskpulse.data.entities.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Long): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)

    @Query("UPDATE rules SET isActive = :isActive WHERE id = :id")
    suspend fun setRuleActive(id: Long, isActive: Boolean)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("SELECT COUNT(*) FROM rules WHERE isActive = 1")
    suspend fun getActiveRuleCount(): Int
}
