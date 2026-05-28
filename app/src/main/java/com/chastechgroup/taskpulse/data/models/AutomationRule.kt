package com.chastechgroup.taskpulse.data.models

data class AutomationRule(
    val id: Long = 0,
    val name: String,
    val description: String,
    val action: CommandAction,
    val targetApps: List<String>,
    val targetCategory: AppCategory?,
    val durationSeconds: Long,
    val trigger: CommandTrigger,
    val triggerApp: String?,
    val triggerUsageSeconds: Long,
    val mode: FocusMode?,
    val isActive: Boolean,
    val createdAt: Long,
    val pointsCost: Int,
    val expiresAt: Long = 0L
)
