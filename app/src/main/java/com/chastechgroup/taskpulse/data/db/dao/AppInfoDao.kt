package com.chastechgroup.taskpulse.data.db.dao

import androidx.room.*
import com.chastechgroup.taskpulse.data.entities.AppInfoEntity
import com.chastechgroup.taskpulse.data.entities.BlockedAppEntity
import com.chastechgroup.taskpulse.data.entities.NotificationFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    @Query("SELECT * FROM apps WHERE isInstalled = 1 ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM apps WHERE category = :category AND isInstalled = 1")
    fun getAppsByCategory(category: String): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getAppByPackage(packageName: String): AppInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppInfoEntity>)

    @Update
    suspend fun updateApp(app: AppInfoEntity)

    @Query("UPDATE apps SET category = :category WHERE packageName = :packageName")
    suspend fun updateCategory(packageName: String, category: String)

    @Query("UPDATE apps SET isBlocked = :blocked WHERE packageName = :packageName")
    suspend fun setAppBlocked(packageName: String, blocked: Boolean)

    @Query("UPDATE apps SET dailyUsageSeconds = :usage, lastUsedTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun updateUsage(packageName: String, usage: Long, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(blockedApp: BlockedAppEntity)

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName AND unblockAt > :now")
    suspend fun getActiveBlock(packageName: String, now: Long): BlockedAppEntity?

    @Query("SELECT * FROM blocked_apps WHERE unblockAt > :now")
    fun getActiveBlocks(now: Long): Flow<List<BlockedAppEntity>>

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun removeBlock(packageName: String)

    @Query("DELETE FROM blocked_apps WHERE unblockAt < :now")
    suspend fun clearExpiredBlocks(now: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationFilter(filter: NotificationFilterEntity)

    @Query("SELECT * FROM notification_filters WHERE packageName = :packageName AND muteUntil > :now")
    suspend fun getActiveFilter(packageName: String, now: Long): NotificationFilterEntity?

    @Query("DELETE FROM notification_filters WHERE packageName = :packageName")
    suspend fun removeNotificationFilter(packageName: String)

    @Query("DELETE FROM notification_filters WHERE muteUntil < :now")
    suspend fun clearExpiredFilters(now: Long)
}
