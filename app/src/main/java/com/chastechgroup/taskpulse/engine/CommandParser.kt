package com.chastechgroup.taskpulse.engine

import com.chastechgroup.taskpulse.data.models.*
import java.util.Calendar
import java.util.regex.Pattern

object CommandParser {

    // ── App aliases ─────────────────────────────────────────────────────
    private val APP_MAP = mapOf(
        "instagram" to "com.instagram.android",
        "insta" to "com.instagram.android",
        "ig" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "fb" to "com.facebook.katana",
        "tiktok" to "com.zhiliaoapp.musically",
        "tik tok" to "com.zhiliaoapp.musically",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "youtube" to "com.google.android.youtube",
        "yt" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "wa" to "com.whatsapp",
        "snapchat" to "com.snapchat.android",
        "snap" to "com.snapchat.android",
        "reddit" to "com.reddit.frontpage",
        "telegram" to "org.telegram.messenger",
        "linkedin" to "com.linkedin.android",
        "pinterest" to "com.pinterest",
        "discord" to "com.discord",
        "twitch" to "tv.twitch.android.app",
        "netflix" to "com.netflix.mediaclient",
        "spotify" to "com.spotify.music",
        "gmail" to "com.google.android.gm",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "messages" to "com.google.android.apps.messaging",
        "phone" to "com.google.android.dialer"
    )

    // ── Category keywords ─────────────────────────────────────────────
    private val CATEGORY_MAP = mapOf(
        "social" to AppCategory.SOCIAL,
        "social media" to AppCategory.SOCIAL,
        "social apps" to AppCategory.SOCIAL,
        "entertainment" to AppCategory.ENTERTAINMENT,
        "gaming" to AppCategory.GAMING,
        "games" to AppCategory.GAMING,
        "productivity" to AppCategory.PRODUCTIVITY,
        "communication" to AppCategory.COMMUNICATION,
        "news" to AppCategory.NEWS,
        "all apps" to AppCategory.ALL,
        "everything" to AppCategory.ALL
    )

    // ── Block action keywords ─────────────────────────────────────────
    private val BLOCK_KEYWORDS = listOf(
        "block", "stop", "disable", "restrict", "prevent", "ban",
        "don't let me use", "dont let me use", "don't allow me",
        "dont allow me", "no more", "turn off", "lock", "pause"
    )

    // ── Unblock action keywords ───────────────────────────────────────
    private val UNBLOCK_KEYWORDS = listOf(
        "unblock", "allow", "enable", "unlock", "let me use",
        "allow me to use", "turn on", "permit", "restore"
    )

    // ── Mute notification keywords ────────────────────────────────────
    private val MUTE_KEYWORDS = listOf(
        "mute", "silence", "quiet", "suppress", "hide notifications",
        "stop notifications", "pause notifications", "no notifications",
        "don't show notifications", "dont show notifications", "freeze notifications"
    )

    // ── Mode keywords ─────────────────────────────────────────────────
    private val MODE_MAP = mapOf(
        "focus mode" to FocusMode.FOCUS,
        "focus" to FocusMode.FOCUS,
        "study mode" to FocusMode.STUDY,
        "study" to FocusMode.STUDY,
        "study time" to FocusMode.STUDY,
        "work mode" to FocusMode.WORK,
        "work" to FocusMode.WORK,
        "working" to FocusMode.WORK,
        "sleep mode" to FocusMode.SLEEP,
        "sleep" to FocusMode.SLEEP,
        "bedtime" to FocusMode.SLEEP,
        "do not disturb" to FocusMode.SLEEP,
        "dnd" to FocusMode.SLEEP
    )

    // ── Time patterns ─────────────────────────────────────────────────
    private val TIME_PATTERNS = listOf(
        Pattern.compile("(\\d+)\\s*hours?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*h\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*minutes?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*mins?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*m\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*seconds?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*secs?\\b", Pattern.CASE_INSENSITIVE)
    )
    private val UNTIL_PATTERN =
        Pattern.compile("until\\s+(\\d+)(?::(\\d+))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
    private val FOR_AWHILE_PATTERN =
        Pattern.compile("for\\s+a\\s+while|for\\s+now|for\\s+some\\s+time", Pattern.CASE_INSENSITIVE)

    // ── Trigger patterns ─────────────────────────────────────────────
    private val WHEN_OPEN_PATTERN =
        Pattern.compile("when\\s+(?:i\\s+)?open\\s+(\\w+)", Pattern.CASE_INSENSITIVE)
    private val AFTER_USAGE_PATTERN =
        Pattern.compile("after\\s+(\\d+)\\s*minutes?\\s+(?:of\\s+use)?", Pattern.CASE_INSENSITIVE)

    // ── Main parse function ──────────────────────────────────────────
    fun parse(input: String): ParsedCommand {
        val raw = input.trim()
        val lower = raw.lowercase()

        if (raw.isBlank()) return ParsedCommand(
            action = CommandAction.UNKNOWN,
            rawInput = raw,
            isValid = false,
            errorMessage = "Empty command"
        )

        val action = detectAction(lower)
        val targetApps = detectApps(lower)
        val targetCategory = detectCategory(lower)
        val duration = parseDuration(lower)
        val untilTs = parseUntilTime(lower)
        val mode = detectMode(lower)
        val trigger = detectTrigger(lower)
        val triggerApp = detectTriggerApp(lower)
        val triggerUsage = parseTriggerUsage(lower)

        return ParsedCommand(
            action = action,
            targetApps = targetApps,
            targetCategory = targetCategory,
            durationSeconds = duration,
            untilTimestamp = untilTs,
            trigger = trigger,
            triggerApp = triggerApp,
            triggerUsageSeconds = triggerUsage,
            mode = mode,
            rawInput = raw,
            isValid = action != CommandAction.UNKNOWN,
            errorMessage = if (action == CommandAction.UNKNOWN) "Couldn't understand command" else ""
        )
    }

    // ── Action Detection ────────────────────────────────────────────
    private fun detectAction(lower: String): CommandAction {
        if (MODE_MAP.keys.any { lower.contains(it) } &&
            (lower.contains("enable") || lower.contains("activate") ||
             lower.contains("start") || lower.contains("turn on") ||
             MODE_MAP.keys.any { lower.startsWith(it) })) {
            return CommandAction.ACTIVATE_MODE
        }
        if (MUTE_KEYWORDS.any { lower.contains(it) }) return CommandAction.MUTE_NOTIFICATIONS
        if (BLOCK_KEYWORDS.any { lower.contains(it) }) return CommandAction.BLOCK_APP
        if (UNBLOCK_KEYWORDS.any { lower.contains(it) }) return CommandAction.UNBLOCK_APP
        if (lower.contains("limit") || lower.contains("set limit") ||
            lower.contains("only") && lower.contains("minutes")) return CommandAction.SET_TIME_LIMIT
        if (MODE_MAP.keys.any { lower.contains(it) }) return CommandAction.ACTIVATE_MODE
        return CommandAction.UNKNOWN
    }

    // ── App Detection ────────────────────────────────────────────────
    private fun detectApps(lower: String): List<String> {
        val found = mutableListOf<String>()
        APP_MAP.entries.sortedByDescending { it.key.length }.forEach { (name, pkg) ->
            if (lower.contains(name) && pkg !in found) found.add(pkg)
        }
        return found
    }

    // ── Category Detection ────────────────────────────────────────────
    private fun detectCategory(lower: String): AppCategory? =
        CATEGORY_MAP.entries.sortedByDescending { it.key.length }
            .firstOrNull { lower.contains(it.key) }?.value

    // ── Duration Parsing ─────────────────────────────────────────────
    fun parseDuration(lower: String): Long {
        if (FOR_AWHILE_PATTERN.matcher(lower).find()) return 3600L

        var totalSeconds = 0L
        TIME_PATTERNS.forEachIndexed { index, pattern ->
            val matcher = pattern.matcher(lower)
            while (matcher.find()) {
                val value = matcher.group(1)?.toLongOrNull() ?: continue
                totalSeconds += when (index) {
                    0, 1 -> value * 3600      // hours
                    2, 3, 4 -> value * 60     // minutes
                    5, 6 -> value             // seconds
                    else -> 0
                }
            }
        }
        return totalSeconds
    }

    // ── Until Time Parsing ───────────────────────────────────────────
    private fun parseUntilTime(lower: String): Long {
        val matcher = UNTIL_PATTERN.matcher(lower)
        if (!matcher.find()) return 0L

        val hour = matcher.group(1)?.toIntOrNull() ?: return 0L
        val minute = matcher.group(2)?.toIntOrNull() ?: 0
        val amPm = matcher.group(3)?.lowercase()

        val cal = Calendar.getInstance()
        var h = hour
        if (amPm == "pm" && h < 12) h += 12
        if (amPm == "am" && h == 12) h = 0

        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)

        if (cal.timeInMillis < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    // ── Mode Detection ───────────────────────────────────────────────
    private fun detectMode(lower: String): FocusMode? =
        MODE_MAP.entries.sortedByDescending { it.key.length }
            .firstOrNull { lower.contains(it.key) }?.value

    // ── Trigger Detection ────────────────────────────────────────────
    private fun detectTrigger(lower: String): CommandTrigger {
        if (WHEN_OPEN_PATTERN.matcher(lower).find()) return CommandTrigger.ON_APP_OPEN
        if (AFTER_USAGE_PATTERN.matcher(lower).find()) return CommandTrigger.ON_TIME_LIMIT
        return CommandTrigger.IMMEDIATE
    }

    private fun detectTriggerApp(lower: String): String? {
        val matcher = WHEN_OPEN_PATTERN.matcher(lower)
        if (!matcher.find()) return null
        val appWord = matcher.group(1)?.lowercase() ?: return null
        return APP_MAP[appWord]
    }

    private fun parseTriggerUsage(lower: String): Long {
        val matcher = AFTER_USAGE_PATTERN.matcher(lower)
        if (!matcher.find()) return 0L
        val minutes = matcher.group(1)?.toLongOrNull() ?: return 0L
        return minutes * 60
    }

    // ── Human-Readable Summary ───────────────────────────────────────
    fun summarize(cmd: ParsedCommand): String {
        val appNames = cmd.targetApps.map { pkg ->
            APP_MAP.entries.firstOrNull { it.value == pkg }?.key?.capitalize() ?: pkg
        }
        val apps = when {
            appNames.isNotEmpty() -> appNames.joinToString(", ")
            cmd.targetCategory != null -> "${cmd.targetCategory.name.lowercase()} apps"
            else -> "apps"
        }
        val dur = formatDuration(cmd.durationSeconds)
        return when (cmd.action) {
            CommandAction.BLOCK_APP -> "Block $apps${if (dur.isNotEmpty()) " for $dur" else ""}"
            CommandAction.UNBLOCK_APP -> "Unblock $apps"
            CommandAction.MUTE_NOTIFICATIONS -> "Mute notifications from $apps${if (dur.isNotEmpty()) " for $dur" else ""}"
            CommandAction.UNMUTE_NOTIFICATIONS -> "Unmute notifications from $apps"
            CommandAction.ACTIVATE_MODE -> "${cmd.mode?.name?.lowercase()?.capitalize()} Mode${if (dur.isNotEmpty()) " for $dur" else ""}"
            CommandAction.SET_TIME_LIMIT -> "Limit $apps to $dur per day"
            else -> cmd.rawInput
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return ""
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (mins > 0) append("${mins}m ")
            if (secs > 0 && hours == 0L) append("${secs}s")
        }.trim()
    }

    // ── Points cost calculation ───────────────────────────────────────
    fun calculatePointsCost(cmd: ParsedCommand): Int {
        val baseCost = when (cmd.action) {
            CommandAction.BLOCK_APP -> 5
            CommandAction.MUTE_NOTIFICATIONS -> 3
            CommandAction.ACTIVATE_MODE -> 8
            CommandAction.SET_TIME_LIMIT -> 5
            else -> 2
        }
        val durationMultiplier = when {
            cmd.durationSeconds > 3600 * 4 -> 3
            cmd.durationSeconds > 3600 -> 2
            else -> 1
        }
        return baseCost * durationMultiplier
    }
}

private fun String.capitalize(): String =
    if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
