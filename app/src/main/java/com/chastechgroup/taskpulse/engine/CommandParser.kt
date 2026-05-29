package com.chastechgroup.taskpulse.engine

import com.chastechgroup.taskpulse.data.models.*
import java.util.Calendar
import java.util.regex.Pattern

object CommandParser {

    // ── App aliases ──────────────────────────────────────────────────────
    private val APP_MAP = mapOf(
        // Instagram
        "instagram" to "com.instagram.android", "insta" to "com.instagram.android",
        "ig" to "com.instagram.android", "instagram reels" to "com.instagram.android",
        "insta reels" to "com.instagram.android",
        // Facebook
        "facebook" to "com.facebook.katana", "fb" to "com.facebook.katana",
        "facebook app" to "com.facebook.katana", "meta" to "com.facebook.katana",
        // TikTok
        "tiktok" to "com.zhiliaoapp.musically", "tik tok" to "com.zhiliaoapp.musically",
        "tik-tok" to "com.zhiliaoapp.musically", "douyin" to "com.zhiliaoapp.musically",
        // Twitter / X
        "twitter" to "com.twitter.android", "x" to "com.twitter.android",
        "x app" to "com.twitter.android", "twitter app" to "com.twitter.android",
        // YouTube
        "youtube" to "com.google.android.youtube", "yt" to "com.google.android.youtube",
        "youtube app" to "com.google.android.youtube", "youtube videos" to "com.google.android.youtube",
        // WhatsApp
        "whatsapp" to "com.whatsapp", "wa" to "com.whatsapp",
        "whatsapp messenger" to "com.whatsapp",
        // Snapchat
        "snapchat" to "com.snapchat.android", "snap" to "com.snapchat.android",
        "snaps" to "com.snapchat.android",
        // Reddit
        "reddit" to "com.reddit.frontpage", "reddit app" to "com.reddit.frontpage",
        // Telegram
        "telegram" to "org.telegram.messenger", "tg" to "org.telegram.messenger",
        // LinkedIn
        "linkedin" to "com.linkedin.android", "linked in" to "com.linkedin.android",
        // Pinterest
        "pinterest" to "com.pinterest",
        // Discord
        "discord" to "com.discord", "discord app" to "com.discord",
        // Twitch
        "twitch" to "tv.twitch.android.app",
        // Netflix
        "netflix" to "com.netflix.mediaclient",
        // Spotify
        "spotify" to "com.spotify.music",
        // Gmail
        "gmail" to "com.google.android.gm", "google mail" to "com.google.android.gm",
        // Chrome
        "chrome" to "com.android.chrome", "google chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        // Maps
        "maps" to "com.google.android.apps.maps", "google maps" to "com.google.android.apps.maps",
        // Messages
        "messages" to "com.google.android.apps.messaging",
        "google messages" to "com.google.android.apps.messaging",
        // Phone
        "phone" to "com.google.android.dialer",
        // Threads
        "threads" to "com.instagram.barcelona",
        // BeReal
        "bereal" to "com.bereal.ft", "be real" to "com.bereal.ft",
        // Clubhouse
        "clubhouse" to "com.clubhouse.app",
        // Tumblr
        "tumblr" to "com.tumblr",
        // Quora
        "quora" to "com.quora.android",
        // Amazon
        "amazon" to "com.amazon.mShop.android.shopping",
        // Uber
        "uber" to "com.ubercab",
        // Duolingo
        "duolingo" to "com.duolingo",
        // Candy Crush
        "candy crush" to "com.king.candycrushsaga",
        "candycrush" to "com.king.candycrushsaga",
        // PUBG
        "pubg" to "com.tencent.ig", "pubg mobile" to "com.tencent.ig",
        // Among Us
        "among us" to "com.innersloth.spacemafia"
    )

    // ── Category keywords ────────────────────────────────────────────────
    private val CATEGORY_MAP = mapOf(
        "social" to AppCategory.SOCIAL,
        "social media" to AppCategory.SOCIAL,
        "social apps" to AppCategory.SOCIAL,
        "social networks" to AppCategory.SOCIAL,
        "social network" to AppCategory.SOCIAL,
        "socials" to AppCategory.SOCIAL,
        "entertainment" to AppCategory.ENTERTAINMENT,
        "streaming" to AppCategory.ENTERTAINMENT,
        "video apps" to AppCategory.ENTERTAINMENT,
        "gaming" to AppCategory.GAMING,
        "games" to AppCategory.GAMING,
        "game apps" to AppCategory.GAMING,
        "mobile games" to AppCategory.GAMING,
        "productivity" to AppCategory.PRODUCTIVITY,
        "work apps" to AppCategory.PRODUCTIVITY,
        "communication" to AppCategory.COMMUNICATION,
        "messaging" to AppCategory.COMMUNICATION,
        "messaging apps" to AppCategory.COMMUNICATION,
        "chat apps" to AppCategory.COMMUNICATION,
        "news" to AppCategory.NEWS,
        "news apps" to AppCategory.NEWS,
        "all apps" to AppCategory.ALL,
        "everything" to AppCategory.ALL,
        "all applications" to AppCategory.ALL,
        "every app" to AppCategory.ALL,
        "distracting apps" to AppCategory.SOCIAL,
        "distractions" to AppCategory.SOCIAL,
        "time wasters" to AppCategory.SOCIAL,
        "addictive apps" to AppCategory.SOCIAL,
        "short videos" to AppCategory.ENTERTAINMENT,
        "short form" to AppCategory.ENTERTAINMENT,
        "reels" to AppCategory.ENTERTAINMENT
    )

    // ── Block keywords ───────────────────────────────────────────────────
    private val BLOCK_KEYWORDS = listOf(
        // Direct commands
        "block", "stop", "disable", "restrict", "prevent", "ban", "pause",
        "lock", "freeze", "hide", "remove access", "cut off", "shut down",
        "shut off", "turn off", "deactivate", "suspend", "halt",
        // "Don't let me" variations
        "don't let me use", "dont let me use", "don't let me open",
        "dont let me open", "don't let me access", "dont let me access",
        "don't allow me", "dont allow me", "don't allow me to use",
        "dont allow me to use", "don't allow me to open", "dont allow me to open",
        // "Stop me" variations
        "stop me from using", "stop me from opening", "stop me from accessing",
        "stop me from going on", "stop me from checking",
        // "No more" variations
        "no more", "no more access", "no more time on", "no more scrolling",
        "no more watching", "cut my time", "cut my access",
        // "I want to stop" variations
        "i want to stop using", "i want to stop opening", "i want to quit",
        "i need to stop", "i need to quit", "i should stop", "i should quit",
        "help me stop", "help me quit", "keep me off", "keep me away from",
        // "I'm spending too much time" variations
        "i'm spending too much time on", "im spending too much time on",
        "i spend too much time on", "too much time on", "i waste too much time on",
        // Kill switch
        "kill", "nuke", "end", "close", "close off",
        // Addiction framing
        "i'm addicted to", "im addicted to", "addicted to", "i can't stop using",
        "i cant stop using", "i can't stop opening", "cant stop opening",
        // Productivity framing
        "i need to focus away from", "distract me less from", "remove distraction",
        "remove distractions", "no distractions", "limit my use of",
        // Put away
        "put away", "take away", "take off", "get rid of",
        "go cold turkey on", "cold turkey"
    )

    // ── Unblock keywords ─────────────────────────────────────────────────
    private val UNBLOCK_KEYWORDS = listOf(
        "unblock", "allow", "enable", "unlock", "restore", "permit",
        "turn on", "give me access", "re-enable", "reenable", "let me use",
        "allow me to use", "allow me to open", "allow me to access",
        "i want to use", "i need to use", "give back access",
        "i need access to", "open up", "bring back", "reactivate",
        "deactivate restriction", "remove block", "remove restriction",
        "remove the block", "remove the restriction", "lift the block",
        "lift restriction", "end block", "stop blocking", "stop restricting",
        "let me back on", "let me back in", "i finished", "i'm done",
        "im done", "break's over", "break is over"
    )

    // ── Mute keywords ────────────────────────────────────────────────────
    private val MUTE_KEYWORDS = listOf(
        "mute", "silence", "quiet", "suppress", "pause notifications",
        "hide notifications", "stop notifications", "no notifications",
        "block notifications", "turn off notifications", "disable notifications",
        "don't notify", "dont notify", "no alerts", "no pings",
        "no buzzes", "stop buzzing", "stop pinging", "stop alerting",
        "freeze notifications", "snooze notifications", "dnd", "do not disturb",
        "don't disturb me", "dont disturb me", "don't bother me",
        "dont bother me", "no interruptions", "no interrupts",
        "i don't want notifications", "i dont want notifications",
        "stop notifying me", "stop alerting me", "kill notifications",
        "kill alerts", "shut up", "shush", "shhh"
    )

    // ── Mode keywords ────────────────────────────────────────────────────
    private val MODE_MAP = mapOf(
        // Focus
        "focus mode" to FocusMode.FOCUS, "focus" to FocusMode.FOCUS,
        "focus time" to FocusMode.FOCUS, "focus session" to FocusMode.FOCUS,
        "deep focus" to FocusMode.FOCUS, "deep work" to FocusMode.FOCUS,
        "flow state" to FocusMode.FOCUS, "concentration mode" to FocusMode.FOCUS,
        "concentrate" to FocusMode.FOCUS, "concentration" to FocusMode.FOCUS,
        "no distraction mode" to FocusMode.FOCUS, "zen mode" to FocusMode.FOCUS,
        "laser focus" to FocusMode.FOCUS, "hyperfocus" to FocusMode.FOCUS,
        // Study
        "study mode" to FocusMode.STUDY, "study" to FocusMode.STUDY,
        "study time" to FocusMode.STUDY, "study session" to FocusMode.STUDY,
        "studying" to FocusMode.STUDY, "homework mode" to FocusMode.STUDY,
        "homework" to FocusMode.STUDY, "exam mode" to FocusMode.STUDY,
        "exam prep" to FocusMode.STUDY, "revision mode" to FocusMode.STUDY,
        "revising" to FocusMode.STUDY, "cramming" to FocusMode.STUDY,
        "reading mode" to FocusMode.STUDY, "learning mode" to FocusMode.STUDY,
        "class mode" to FocusMode.STUDY, "school mode" to FocusMode.STUDY,
        "lecture mode" to FocusMode.STUDY,
        // Work
        "work mode" to FocusMode.WORK, "work" to FocusMode.WORK,
        "work time" to FocusMode.WORK, "work session" to FocusMode.WORK,
        "working" to FocusMode.WORK, "office mode" to FocusMode.WORK,
        "office hours" to FocusMode.WORK, "productivity mode" to FocusMode.WORK,
        "professional mode" to FocusMode.WORK, "meeting mode" to FocusMode.WORK,
        "in a meeting" to FocusMode.WORK, "on a call" to FocusMode.WORK,
        "pomodoro" to FocusMode.WORK, "sprint mode" to FocusMode.WORK,
        "hustle mode" to FocusMode.WORK, "grind mode" to FocusMode.WORK,
        "boss mode" to FocusMode.WORK,
        // Sleep
        "sleep mode" to FocusMode.SLEEP, "sleep" to FocusMode.SLEEP,
        "bedtime" to FocusMode.SLEEP, "bed time" to FocusMode.SLEEP,
        "sleeping" to FocusMode.SLEEP, "night mode" to FocusMode.SLEEP,
        "goodnight" to FocusMode.SLEEP, "good night" to FocusMode.SLEEP,
        "going to bed" to FocusMode.SLEEP, "going to sleep" to FocusMode.SLEEP,
        "time to sleep" to FocusMode.SLEEP, "time for bed" to FocusMode.SLEEP,
        "winding down" to FocusMode.SLEEP, "wind down" to FocusMode.SLEEP,
        "night routine" to FocusMode.SLEEP, "rest mode" to FocusMode.SLEEP,
        "rest time" to FocusMode.SLEEP, "nap time" to FocusMode.SLEEP,
        "nap mode" to FocusMode.SLEEP, "taking a nap" to FocusMode.SLEEP
    )

    // ── Time patterns ────────────────────────────────────────────────────
    private val TIME_PATTERNS = listOf(
        Pattern.compile("(\\d+)\\s*hours?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*h\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*minutes?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*mins?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*m\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*seconds?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*secs?\\b", Pattern.CASE_INSENSITIVE)
    )

    // ── Fuzzy duration phrases → seconds ────────────────────────────────
    private val FUZZY_DURATIONS = listOf(
        // Short
        Pair(Pattern.compile("for\\s+a\\s+sec(ond)?|just\\s+a\\s+sec|real\\s+quick", Pattern.CASE_INSENSITIVE), 60L),
        Pair(Pattern.compile("for\\s+a\\s+bit|for\\s+a\\s+little\\s+(bit|while)|for\\s+a\\s+moment", Pattern.CASE_INSENSITIVE), 900L),
        Pair(Pattern.compile("for\\s+a\\s+few\\s+minutes?|for\\s+some\\s+minutes?", Pattern.CASE_INSENSITIVE), 900L),
        Pair(Pattern.compile("for\\s+a\\s+while|for\\s+now|for\\s+some\\s+time", Pattern.CASE_INSENSITIVE), 3600L),
        Pair(Pattern.compile("for\\s+a\\s+long\\s+(time|while)|for\\s+ages|for\\s+a\\s+very\\s+long", Pattern.CASE_INSENSITIVE), 14400L),
        // All day / rest of day
        Pair(Pattern.compile("all\\s+day|for\\s+the\\s+(rest\\s+of\\s+the\\s+)?day|today|the\\s+whole\\s+day|entire\\s+day", Pattern.CASE_INSENSITIVE), 86400L),
        Pair(Pattern.compile("all\\s+night|for\\s+the\\s+(rest\\s+of\\s+the\\s+)?night|tonight|the\\s+whole\\s+night", Pattern.CASE_INSENSITIVE), 28800L),
        Pair(Pattern.compile("all\\s+morning|this\\s+morning|for\\s+the\\s+morning", Pattern.CASE_INSENSITIVE), 10800L),
        Pair(Pattern.compile("all\\s+afternoon|this\\s+afternoon|for\\s+the\\s+afternoon", Pattern.CASE_INSENSITIVE), 14400L),
        Pair(Pattern.compile("all\\s+evening|this\\s+evening|for\\s+the\\s+evening", Pattern.CASE_INSENSITIVE), 10800L),
        // Pomodoro / session framing
        Pair(Pattern.compile("one\\s+pomodoro|a\\s+pomodoro", Pattern.CASE_INSENSITIVE), 1500L),
        Pair(Pattern.compile("a\\s+sprint|one\\s+sprint", Pattern.CASE_INSENSITIVE), 5400L),
        // Meal framing
        Pair(Pattern.compile("during\\s+(lunch|dinner|breakfast)|while\\s+i\\s+eat|at\\s+(lunch|dinner|breakfast)", Pattern.CASE_INSENSITIVE), 3600L),
        // Exercise
        Pair(Pattern.compile("during\\s+(my\\s+)?workout|while\\s+i\\s+(work\\s+out|exercise|run|gym)|at\\s+the\\s+gym", Pattern.CASE_INSENSITIVE), 5400L),
        // Commute
        Pair(Pattern.compile("during\\s+(my\\s+)?commute|on\\s+the\\s+(bus|train|subway|metro)", Pattern.CASE_INSENSITIVE), 3600L),
        // Forever-ish
        Pair(Pattern.compile("forever|permanently|indefinitely|until\\s+further\\s+notice|until\\s+i\\s+say\\s+so", Pattern.CASE_INSENSITIVE), 31536000L)
    )

    private val UNTIL_PATTERN =
        Pattern.compile("until\\s+(\\d+)(?::(\\d+))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)

    // ── Trigger patterns ─────────────────────────────────────────────────
    private val WHEN_OPEN_PATTERN =
        Pattern.compile("when\\s+(?:i\\s+)?(?:open|launch|start|use|go\\s+on|visit)\\s+(\\w+)", Pattern.CASE_INSENSITIVE)
    private val AFTER_USAGE_PATTERN =
        Pattern.compile("after\\s+(\\d+)\\s*minutes?\\s*(?:of\\s+(?:use|usage|screen\\s+time))?", Pattern.CASE_INSENSITIVE)

    // ── Main parse ───────────────────────────────────────────────────────
    fun parse(input: String): ParsedCommand {
        val raw = input.trim()
        val lower = raw.lowercase()

        if (raw.isBlank()) return ParsedCommand(
            action = CommandAction.UNKNOWN, rawInput = raw,
            isValid = false, errorMessage = "Empty command"
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
            action = action, targetApps = targetApps,
            targetCategory = targetCategory, durationSeconds = duration,
            untilTimestamp = untilTs, trigger = trigger,
            triggerApp = triggerApp, triggerUsageSeconds = triggerUsage,
            mode = mode, rawInput = raw,
            isValid = action != CommandAction.UNKNOWN,
            errorMessage = if (action == CommandAction.UNKNOWN) "Couldn't understand command" else ""
        )
    }

    // ── Action Detection ─────────────────────────────────────────────────
    private fun detectAction(lower: String): CommandAction {
        // Mode activation takes priority
        if (MODE_MAP.keys.any { lower.contains(it) } &&
            (lower.contains("enable") || lower.contains("activate") ||
             lower.contains("start") || lower.contains("turn on") ||
             lower.contains("put on") || lower.contains("set") ||
             lower.contains("begin") || lower.contains("i'm going to") ||
             lower.contains("im going to") || lower.contains("i am") ||
             lower.contains("i need") || lower.contains("going into") ||
             MODE_MAP.keys.any { lower.startsWith(it) })) {
            return CommandAction.ACTIVATE_MODE
        }
        if (MUTE_KEYWORDS.any { lower.contains(it) }) return CommandAction.MUTE_NOTIFICATIONS
        if (BLOCK_KEYWORDS.any { lower.contains(it) }) return CommandAction.BLOCK_APP
        if (UNBLOCK_KEYWORDS.any { lower.contains(it) }) return CommandAction.UNBLOCK_APP
        if (lower.contains("limit") || lower.contains("set limit") ||
            lower.contains("set a limit") || lower.contains("set time limit") ||
            lower.contains("only") && lower.contains("minutes") ||
            lower.contains("only") && lower.contains("hours") ||
            lower.contains("max") || lower.contains("maximum") ||
            lower.contains("no more than") || lower.contains("cap my") ||
            lower.contains("cap at")) return CommandAction.SET_TIME_LIMIT
        if (MODE_MAP.keys.any { lower.contains(it) }) return CommandAction.ACTIVATE_MODE
        return CommandAction.UNKNOWN
    }

    // ── App Detection ─────────────────────────────────────────────────────
    private fun detectApps(lower: String): List<String> {
        val found = mutableListOf<String>()
        APP_MAP.entries.sortedByDescending { it.key.length }.forEach { (name, pkg) ->
            if (lower.contains(name) && pkg !in found) found.add(pkg)
        }
        return found
    }

    // ── Category Detection ────────────────────────────────────────────────
    private fun detectCategory(lower: String): AppCategory? =
        CATEGORY_MAP.entries.sortedByDescending { it.key.length }
            .firstOrNull { lower.contains(it.key) }?.value

    // ── Duration Parsing ──────────────────────────────────────────────────
    fun parseDuration(lower: String): Long {
        // Check fuzzy duration phrases first
        for ((pattern, seconds) in FUZZY_DURATIONS) {
            if (pattern.matcher(lower).find()) return seconds
        }

        var totalSeconds = 0L
        TIME_PATTERNS.forEachIndexed { index, pattern ->
            val matcher = pattern.matcher(lower)
            while (matcher.find()) {
                val value = matcher.group(1)?.toLongOrNull() ?: continue
                totalSeconds += when (index) {
                    0, 1 -> value * 3600
                    2, 3, 4 -> value * 60
                    5, 6 -> value
                    else -> 0
                }
            }
        }
        return totalSeconds
    }

    // ── Until Time Parsing ────────────────────────────────────────────────
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
        if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    // ── Mode Detection ────────────────────────────────────────────────────
    private fun detectMode(lower: String): FocusMode? =
        MODE_MAP.entries.sortedByDescending { it.key.length }
            .firstOrNull { lower.contains(it.key) }?.value

    // ── Trigger Detection ─────────────────────────────────────────────────
    private fun detectTrigger(lower: String): CommandTrigger {
        if (WHEN_OPEN_PATTERN.matcher(lower).find()) return CommandTrigger.ON_APP_OPEN
        if (AFTER_USAGE_PATTERN.matcher(lower).find()) return CommandTrigger.ON_TIME_LIMIT
        return CommandTrigger.IMMEDIATE
    }

    private fun detectTriggerApp(lower: String): String? {
        val matcher = WHEN_OPEN_PATTERN.matcher(lower)
        if (!matcher.find()) return null
        return APP_MAP[matcher.group(1)?.lowercase()]
    }

    private fun parseTriggerUsage(lower: String): Long {
        val matcher = AFTER_USAGE_PATTERN.matcher(lower)
        if (!matcher.find()) return 0L
        return (matcher.group(1)?.toLongOrNull() ?: 0L) * 60
    }

    // ── Human-Readable Summary ─────────────────────────────────────────────
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
