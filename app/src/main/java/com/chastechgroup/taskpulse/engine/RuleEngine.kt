package com.chastechgroup.taskpulse.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
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

    private val db          = AppDatabase.getInstance(context)
    private val scope       = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    val SOCIAL_APPS = listOf(
        "com.instagram.android", "com.facebook.katana", "com.zhiliaoapp.musically",
        "com.twitter.android", "com.snapchat.android", "com.reddit.frontpage",
        "com.pinterest", "com.linkedin.android"
    )
    val ENTERTAINMENT_APPS = listOf(
        "com.netflix.mediaclient", "com.spotify.music",
        "tv.twitch.android.app", "com.google.android.youtube"
    )

    // ── Block detail model ───────────────────────────────────────────────
    data class BlockDetails(
        val appName: String,
        val expiresAt: Long,
        val ruleName: String,
        val blockReason: String
    )

    // ── Get details of an active block for overlay display ───────────────
    suspend fun getBlockDetails(packageName: String): BlockDetails? {
        val now   = System.currentTimeMillis()
        val block = db.appInfoDao().getActiveBlock(packageName, now) ?: return null
        val rule  = db.ruleDao().getRuleById(block.ruleId)
        return BlockDetails(
            appName     = APP_DISPLAY_NAMES[packageName] ?: packageName.split(".").last()
                .replaceFirstChar { it.uppercase() },
            expiresAt   = block.expiresAt,
            ruleName    = rule?.name ?: "Blocked by TaskPulse",
            blockReason = rule?.description ?: ""
        )
    }

    // ── Execute a parsed command ─────────────────────────────────────────
    fun execute(cmd: ParsedCommand, onResult: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val targets = resolveTargets(cmd)
                val ruleId  = saveRule(cmd)
                val now     = System.currentTimeMillis()
                val expiry  = when {
                    cmd.untilTimestamp > 0  -> cmd.untilTimestamp
                    cmd.durationSeconds > 0 -> now + cmd.durationSeconds * 1000L
                    else                    -> now + 3_600_000L
                }

                when (cmd.action) {

                    CommandAction.BLOCK_APP -> {
                        applyBlock(targets, now, expiry, ruleId, cmd.summarize())
                        onResult(true, "✓ ${targets.size} app(s) blocked")
                    }

                    CommandAction.SCHEDULE_BLOCK -> {
                        val delayMs     = cmd.delaySeconds * 1000L
                        val blockStart  = now + delayMs
                        val blockExpiry = if (cmd.durationSeconds > 0)
                            blockStart + cmd.durationSeconds * 1000L
                            else blockStart + 3_600_000L

                        mainHandler.postDelayed({
                            scope.launch {
                                try { applyBlock(targets, blockStart, blockExpiry, ruleId, cmd.summarize()) }
                                catch (_: Exception) {}
                            }
                        }, delayMs)

                        val delayLabel = CommandParser.formatDuration(cmd.delaySeconds)
                        val durLabel   = CommandParser.formatDuration(cmd.durationSeconds)
                        onResult(true, buildString {
                            append("⏱ Block scheduled")
                            if (delayLabel.isNotEmpty()) append(" in $delayLabel")
                            if (durLabel.isNotEmpty()) append(" for $durLabel")
                        })
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
                        val label = cmd.mode?.name?.lowercase()
                            ?.replaceFirstChar { it.uppercase() } ?: "Focus"
                        onResult(true, "✓ $label Mode active")
                    }

                    CommandAction.SET_TIME_LIMIT -> onResult(true, "✓ Time limit set")

                    else -> onResult(false, "Command not recognized")
                }

                db.appInfoDao().clearExpiredBlocks(now)
                db.appInfoDao().clearExpiredFilters(now)

            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    private suspend fun applyBlock(
        targets: List<String>, startTime: Long,
        expiry: Long, ruleId: Long, label: String
    ) {
        targets.forEach { pkg ->
            db.appInfoDao().insertBlockedApp(
                BlockedAppEntity(pkg, startTime, expiry, ruleId, label)
            )
            db.appInfoDao().setAppBlocked(pkg, true)
        }
        db.sessionDao().insertSession(
            SessionEntity(0, ruleId, targets.firstOrNull() ?: "", startTime, 0, true, "BLOCK")
        )
    }

    private fun resolveTargets(cmd: ParsedCommand): List<String> {
        val apps = cmd.targetApps.toMutableList()
        cmd.targetCategory?.let { cat ->
            apps.addAll(when (cat) {
                AppCategory.SOCIAL        -> SOCIAL_APPS
                AppCategory.ENTERTAINMENT -> ENTERTAINMENT_APPS
                AppCategory.ALL           -> SOCIAL_APPS + ENTERTAINMENT_APPS
                else                      -> emptyList()
            })
        }
        return apps.distinct()
    }

    private fun getModeApps(mode: FocusMode?): List<String> = when (mode) {
        FocusMode.STUDY, FocusMode.FOCUS -> SOCIAL_APPS + ENTERTAINMENT_APPS
        FocusMode.WORK  -> SOCIAL_APPS + listOf("com.zhiliaoapp.musically", "tv.twitch.android.app")
        FocusMode.SLEEP -> SOCIAL_APPS + ENTERTAINMENT_APPS
        else            -> SOCIAL_APPS
    }

    private suspend fun saveRule(cmd: ParsedCommand): Long {
        val entity = RuleEntity(
            name                = CommandParser.summarize(cmd),
            description         = cmd.rawInput,
            action              = cmd.action.name,
            targetApps          = cmd.targetApps.joinToString(","),
            targetCategory      = cmd.targetCategory?.name,
            durationSeconds     = cmd.durationSeconds,
            trigger             = cmd.trigger.name,
            triggerApp          = cmd.triggerApp,
            triggerUsageSeconds = cmd.triggerUsageSeconds,
            mode                = cmd.mode?.name,
            isActive            = true,
            createdAt           = System.currentTimeMillis(),
            pointsCost          = cmd.pointsCost,
            expiresAt           = if (cmd.durationSeconds > 0)
                System.currentTimeMillis() + cmd.durationSeconds * 1000L else 0L
        )
        return db.ruleDao().insertRule(entity)
    }

    suspend fun isAppBlocked(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        return db.appInfoDao().getActiveBlock(packageName, now) != null
    }

    suspend fun areNotificationsMuted(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        return db.appInfoDao().getActiveFilter(packageName, now) != null
    }

    fun ParsedCommand.summarize(): String = CommandParser.summarize(this)
    val ParsedCommand.pointsCost: Int get() = CommandParser.calculatePointsCost(this)

    companion object {
        val APP_DISPLAY_NAMES = mapOf(
            "com.instagram.android"          to "Instagram",
            "com.facebook.katana"            to "Facebook",
            "com.zhiliaoapp.musically"       to "TikTok",
            "com.twitter.android"            to "Twitter / X",
            "com.google.android.youtube"     to "YouTube",
            "com.whatsapp"                   to "WhatsApp",
            "com.snapchat.android"           to "Snapchat",
            "com.reddit.frontpage"           to "Reddit",
            "org.telegram.messenger"         to "Telegram",
            "com.linkedin.android"           to "LinkedIn",
            "com.pinterest"                  to "Pinterest",
            "com.discord"                    to "Discord",
            "tv.twitch.android.app"          to "Twitch",
            "com.netflix.mediaclient"        to "Netflix",
            "com.spotify.music"              to "Spotify",
            "com.google.android.gm"          to "Gmail",
            "com.android.chrome"             to "Chrome",
            "com.instagram.barcelona"        to "Threads",
            "com.duolingo"                   to "Duolingo",
            "com.king.candycrushsaga"        to "Candy Crush",
            "com.tencent.ig"                 to "PUBG Mobile",
            "com.bereal.ft"                  to "BeReal"
        )
    }
}
