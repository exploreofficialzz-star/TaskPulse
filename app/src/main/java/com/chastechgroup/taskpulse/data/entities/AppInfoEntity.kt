package com.chastechgroup.taskpulse.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String,
    val isInstalled: Boolean,
    val dailyUsageSeconds: Long = 0L,
    val lastUsedTimestamp: Long = 0L,
    val isBlocked: Boolean = false,
    val iconBase64: String = ""
)
