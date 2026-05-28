package com.chastechgroup.taskpulse.engine

import android.content.Context
import com.chastechgroup.taskpulse.data.db.AppDatabase
import com.chastechgroup.taskpulse.data.entities.BlockedAppEntity
import com.chastechgroup.taskpulse.data.entities.NotificationFilterEntity
import com.chastechgroup.taskpulse.data.entities.RuleEntity
import com.chastechgroup.taskpulse.data.entities.SessionEntity
import com.chastechgroup.taskpulse.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RuleEngine(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Social media app packages
    val SOCIAL_APPS = listOf(
        "com.instagram.android", "com.facebook.katana", "com.zhiliaoapp.musically",
        "com.twitter.android", "com.snapchat.android", "com.reddit.frontpage",
        "com.pinterest", "com.linkedin.android"
    )

    val ENTERTAINMENT_APPS = listOf(
        "com.netflix.mediaclient", "com.spotify.music", "tv.twitch.android.app",
        "com.google.android.youtube"
    )

    // ── Execute a parsed command ─────────────────────────────────────
    fun execute(cmd: ParsedCommand, onResult: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val targets = resolveTargets(cmd)
                val ruleId = saveRule(cmd)
                val now = System.currentTimeMillis()
                val expiry = when {
                    cmd.untilTimestamp > 0 -> cmd.untilTimestamp
                    cmd.durationSeconds > 0 -> now + cmd.durationSeconds * 1000
                    else -> now + 3600_000L
                }

                when (cmd.action) {
                    CommandAction.BLOCK_APP -> {
                        targets.forEach { pkg ->
                            db.appInfoDao().insertBlockedApp(
                                BlockedAppEntity(pkg, now, expiry, ruleId, cmd.summarize())
                            )
                            db.appInfoDao().setAppBlocked(pkg, true)
                        }
                        db.sessionDao().insertSession(
                            SessionEntity(0, ruleId, targets.firstOrNull() ?: "", now, 0, true, "BLOCK")
                        )
                        onResult(true, "✓ ${targets.size} app(s) blocked")
                    }
                    CommandAction.MUTE_NOTIFICATIONS -> {
                        targets.forEach { pkg ->
                            db.appInfoDao().insertNotificationFilter(
                                NotificationFilterEntity(0, pkg, true, expiry, ruleId)
                            )
                        }
                        onResult(true, "✓ Notifications muted")
                    }
                    CommandAction.UNBLOCK_APP -> {
                        targets.forEach { pkg ->
                            db.appInfoDao().removeBlock(pkg)
                            db.appInfoDao().setAppBlocked(pkg, false)
                        }
                        onResult(true, "✓ Apps unblocked")
                    }
                    CommandAction.UNMUTE_NOTIFICATIONS -> {
                        targets.forEach { db.appInfoDao().removeNotificationFilter(it) }
                        onResult(true, "✓ Notifications restored")
                    }
                    CommandAction.ACTIVATE_MODE -> {
                        val modeApps = getModeApps(cmd.mode)
                        modeApps.forEach { pkg ->
                            db.appInfoDao().insertBlockedApp(
                                BlockedAppEntity(pkg, now, expiry, ruleId, "${cmd.mode} mode")
                            )
                            db.appInfoDao().setAppBlocked(pkg, true)
                            db.appInfoDao().insertNotificationFilter(
                                NotificationFilterEntity(0, pkg, true, expiry, ruleId)
                            )
                        }
                        onResult(true, "✓ ${cmd.mode?.name?.lowercase()?.replaceFirstChar { it.uppercase() }} mode active")
                    }
                    CommandAction.SET_TIME_LIMIT -> {
                        onResult(true, "✓ Time limit set")
                    }
                    else -> onResult(false, "Command not recognized")
                }

                // Clean up expired blocks
                db.appInfoDao().clearExpiredBlocks(now)
                db.appInfoDao().clearExpiredFilters(now)

            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    private fun resolveTargets(cmd: ParsedCommand): List<String> {
        val apps = cmd.targetApps.toMutableList()
        cmd.targetCategory?.let { cat ->
            apps.addAll(when (cat) {
                AppCategory.SOCIAL -> SOCIAL_APPS
                AppCategory.ENTERTAINMENT -> ENTERTAINMENT_APPS
                AppCategory.ALL -> SOCIAL_APPS + ENTERTAINMENT_APPS
                else -> emptyList()
            })
        }
        return apps.distinct()
    }

    private fun getModeApps(mode: FocusMode?): List<String> = when (mode) {
        FocusMode.STUDY, FocusMode.FOCUS -> SOCIAL_APPS + ENTERTAINMENT_APPS
        FocusMode.WORK -> SOCIAL_APPS + listOf("com.zhiliaoapp.musically", "tv.twitch.android.app")
        FocusMode.SLEEP -> SOCIAL_APPS + ENTERTAINMENT_APPS
        else -> SOCIAL_APPS
    }

    private suspend fun saveRule(cmd: ParsedCommand): Long {
        val entity = RuleEntity(
            name = CommandParser.summarize(cmd),
            description = cmd.rawInput,
            action = cmd.action.name,
            targetApps = cmd.targetApps.joinToString(","),
            targetCategory = cmd.targetCategory?.name,
            durationSeconds = cmd.durationSeconds,
            trigger = cmd.trigger.name,
            triggerApp = cmd.triggerApp,
            triggerUsageSeconds = cmd.triggerUsageSeconds,
            mode = cmd.mode?.name,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            pointsCost = cmd.pointsCost,
            expiresAt = if (cmd.durationSeconds > 0)
                System.currentTimeMillis() + cmd.durationSeconds * 1000 else 0L
        )
        return db.ruleDao().insertRule(entity)
    }

    // ── Check if an app is currently blocked ─────────────────────────
    suspend fun isAppBlocked(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        return db.appInfoDao().getActiveBlock(packageName, now) != null
    }

    // ── Check if notifications should be muted for package ───────────
    suspend fun areNotificationsMuted(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        return db.appInfoDao().getActiveFilter(packageName, now) != null
    }

    fun ParsedCommand.summarize(): String = CommandParser.summarize(this)
    val ParsedCommand.pointsCost: Int get() = CommandParser.calculatePointsCost(this)
}
