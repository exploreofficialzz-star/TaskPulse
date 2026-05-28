package com.chastechgroup.taskpulse.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "rules")
@TypeConverters(RuleConverters::class)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val action: String,
    val targetApps: String,   // JSON array stored as string
    val targetCategory: String?,
    val durationSeconds: Long,
    val trigger: String,
    val triggerApp: String?,
    val triggerUsageSeconds: Long,
    val mode: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val pointsCost: Int,
    val expiresAt: Long = 0L
)

class RuleConverters {
    @TypeConverter
    fun fromString(value: String): List<String> =
        if (value.isEmpty()) emptyList()
        else value.split(",").map { it.trim() }

    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(",")
}
