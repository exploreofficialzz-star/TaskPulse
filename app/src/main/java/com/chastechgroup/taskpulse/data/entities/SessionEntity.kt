package com.chastechgroup.taskpulse.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val packageName: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val isActive: Boolean = true,
    val sessionType: String
)

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val blockedAt: Long,
    val unblockAt: Long,
    val ruleId: Long,
    val reason: String
)

@Entity(tableName = "notification_filters")
data class NotificationFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val isMuted: Boolean,
    val muteUntil: Long,
    val ruleId: Long
)
