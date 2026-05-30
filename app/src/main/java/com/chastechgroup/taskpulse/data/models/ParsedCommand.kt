package com.chastechgroup.taskpulse.data.models

data class ParsedCommand(
    val action: CommandAction,
    val targetApps: List<String> = emptyList(),
    val targetCategory: AppCategory? = null,
    val durationSeconds: Long = 0L,
    val delaySeconds: Long = 0L,
    val untilTimestamp: Long = 0L,
    val trigger: CommandTrigger = CommandTrigger.IMMEDIATE,
    val triggerApp: String? = null,
    val triggerUsageSeconds: Long = 0L,
    val mode: FocusMode? = null,
    val rawInput: String = "",
    val isValid: Boolean = true,
    val errorMessage: String = ""
)

enum class CommandAction {
    BLOCK_APP,
    UNBLOCK_APP,
    MUTE_NOTIFICATIONS,
    UNMUTE_NOTIFICATIONS,
    ACTIVATE_MODE,
    DEACTIVATE_MODE,
    SET_TIME_LIMIT,
    SCHEDULE_BLOCK,
    UNKNOWN
}

enum class CommandTrigger {
    IMMEDIATE,
    ON_APP_OPEN,
    ON_TIME_LIMIT,
    SCHEDULED
}

enum class AppCategory {
    SOCIAL, ENTERTAINMENT, GAMING, PRODUCTIVITY, COMMUNICATION, NEWS, ALL
}

enum class FocusMode {
    FOCUS, STUDY, WORK, SLEEP, CUSTOM
}
