package com.chastechgroup.taskpulse.data.repository

import android.content.Context
import com.chastechgroup.taskpulse.data.db.AppDatabase
import com.chastechgroup.taskpulse.data.entities.*
import com.chastechgroup.taskpulse.data.models.*
import com.chastechgroup.taskpulse.engine.CommandParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskPulseRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)

    // ── Rules ────────────────────────────────────────────────────────
    fun getAllRules(): Flow<List<AutomationRule>> =
        db.ruleDao().getAllRules().map { list -> list.map { it.toModel() } }

    fun getActiveRules(): Flow<List<AutomationRule>> =
        db.ruleDao().getActiveRules().map { list -> list.map { it.toModel() } }

    suspend fun deleteRule(id: Long) = db.ruleDao().deleteRuleById(id)

    suspend fun toggleRule(id: Long, active: Boolean) = db.ruleDao().setRuleActive(id, active)

    // ── Apps ─────────────────────────────────────────────────────────
    fun getAllApps(): Flow<List<AppInfoEntity>> = db.appInfoDao().getAllApps()

    fun getAppsByCategory(category: String): Flow<List<AppInfoEntity>> =
        db.appInfoDao().getAppsByCategory(category)

    suspend fun updateAppCategory(packageName: String, category: String) =
        db.appInfoDao().updateCategory(packageName, category)

    // ── Blocked apps ─────────────────────────────────────────────────
    fun getActiveBlocks(): Flow<List<BlockedAppEntity>> =
        db.appInfoDao().getActiveBlocks(System.currentTimeMillis())

    suspend fun removeBlock(packageName: String) {
        db.appInfoDao().removeBlock(packageName)
        db.appInfoDao().setAppBlocked(packageName, false)
    }

    // ── Sessions / Logs ──────────────────────────────────────────────
    fun getRecentSessions(): Flow<List<SessionEntity>> = db.sessionDao().getRecentSessions()

    // ── Converters ───────────────────────────────────────────────────
    private fun RuleEntity.toModel(): AutomationRule = AutomationRule(
        id = id,
        name = name,
        description = description,
        action = runCatching { CommandAction.valueOf(action) }.getOrDefault(CommandAction.UNKNOWN),
        targetApps = if (targetApps.isEmpty()) emptyList() else targetApps.split(","),
        targetCategory = targetCategory?.let { runCatching { AppCategory.valueOf(it) }.getOrNull() },
        durationSeconds = durationSeconds,
        trigger = runCatching { CommandTrigger.valueOf(trigger) }.getOrDefault(CommandTrigger.IMMEDIATE),
        triggerApp = triggerApp,
        triggerUsageSeconds = triggerUsageSeconds,
        mode = mode?.let { runCatching { FocusMode.valueOf(it) }.getOrNull() },
        isActive = isActive,
        createdAt = createdAt,
        pointsCost = pointsCost,
        expiresAt = expiresAt
    )
}
