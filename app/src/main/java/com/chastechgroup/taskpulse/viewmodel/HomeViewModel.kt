package com.chastechgroup.taskpulse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chastechgroup.taskpulse.data.models.*
import com.chastechgroup.taskpulse.data.repository.TaskPulseRepository
import com.chastechgroup.taskpulse.engine.CommandParser
import com.chastechgroup.taskpulse.engine.RuleEngine
import com.chastechgroup.taskpulse.util.PermissionHelper
import com.chastechgroup.taskpulse.util.PreferencesHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Permission types the app may need ───────────────────────────────────────
enum class RequiredPermissionType {
    ACCESSIBILITY,
    OVERLAY,
    NOTIFICATION_ACCESS,
    USAGE_ACCESS
}

data class PermissionRequest(
    val type: RequiredPermissionType,
    val title: String,
    val reason: String,        // why this command needs it
    val pendingCommand: String // the command to retry after granting
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val commandInput: String = "",
    val parsedPreview: ParsedCommand? = null,
    val lastResult: String = "",
    val isSuccess: Boolean = false,
    val activeRules: List<AutomationRule> = emptyList(),
    val points: Int = 50,
    val showPointsAlert: Boolean = false,
    val permissionRequest: PermissionRequest? = null   // non-null = show permission dialog
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo       = TaskPulseRepository(application)
    private val ruleEngine = RuleEngine(application)
    private val _uiState   = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getActiveRules().collect { rules ->
                _uiState.update { it.copy(activeRules = rules) }
            }
        }
        viewModelScope.launch {
            PreferencesHelper.getPoints(application).collect { pts ->
                _uiState.update { it.copy(points = pts) }
            }
        }
    }

    fun onCommandChanged(input: String) {
        val preview = if (input.length > 3) CommandParser.parse(input) else null
        _uiState.update { it.copy(commandInput = input, parsedPreview = preview) }
    }

    fun runCommand() {
        val input = _uiState.value.commandInput.trim()
        if (input.isEmpty()) return

        val cmd = CommandParser.parse(input)
        if (!cmd.isValid) {
            _uiState.update { it.copy(lastResult = "⚠ ${cmd.errorMessage}", isSuccess = false) }
            return
        }

        // ── Permission gate ──────────────────────────────────────────────
        val missing = getMissingPermission(cmd, input)
        if (missing != null) {
            _uiState.update { it.copy(permissionRequest = missing) }
            return
        }

        // ── Points check ─────────────────────────────────────────────────
        val cost = CommandParser.calculatePointsCost(cmd)
        if (_uiState.value.points < cost) {
            _uiState.update { it.copy(showPointsAlert = true) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val deducted = PreferencesHelper.deductPoints(getApplication(), cost)
            if (!deducted) {
                _uiState.update { it.copy(isLoading = false, showPointsAlert = true) }
                return@launch
            }
            PreferencesHelper.incrementCommands(getApplication())

            ruleEngine.execute(cmd) { success, message ->
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            lastResult   = message,
                            isSuccess    = success,
                            commandInput = if (success) "" else it.commandInput,
                            parsedPreview = null
                        )
                    }
                }
            }
        }
    }

    // ── Called when user returns from settings (onResume / lifecycle) ────────
    fun recheckPermissions() {
        val pending = _uiState.value.permissionRequest ?: return
        val app = getApplication<Application>()
        val stillMissing = when (pending.type) {
            RequiredPermissionType.ACCESSIBILITY      -> !PermissionHelper.hasAccessibilityService(app)
            RequiredPermissionType.OVERLAY            -> !PermissionHelper.hasOverlayPermission(app)
            RequiredPermissionType.NOTIFICATION_ACCESS-> !PermissionHelper.hasNotificationAccess(app)
            RequiredPermissionType.USAGE_ACCESS       -> !PermissionHelper.hasUsageAccess(app)
        }
        if (!stillMissing) {
            // Permission granted — dismiss dialog and auto-run
            _uiState.update { it.copy(permissionRequest = null) }
            runCommand()
        }
    }

    fun dismissPermissionRequest() =
        _uiState.update { it.copy(permissionRequest = null) }

    fun dismissPointsAlert() = _uiState.update { it.copy(showPointsAlert = false) }
    fun clearResult()        = _uiState.update { it.copy(lastResult = "") }

    fun deleteRule(id: Long) = viewModelScope.launch { repo.deleteRule(id) }
    fun toggleRule(id: Long, active: Boolean) =
        viewModelScope.launch { repo.toggleRule(id, active) }

    fun activateQuickMode(mode: FocusMode, durationHours: Int = 1) {
        val suffix = if (durationHours > 1) "s" else ""
        val input  = "${mode.name.lowercase()} mode for $durationHours hour$suffix"
        _uiState.update { it.copy(commandInput = input) }
        runCommand()
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Permission Gate Logic
    //  Maps each action to the permissions it needs, checks them in priority order
    // ──────────────────────────────────────────────────────────────────────────
    private fun getMissingPermission(cmd: ParsedCommand, rawInput: String): PermissionRequest? {
        val app = getApplication<Application>()

        // What each action needs
        val needsAccessibility = cmd.action in listOf(
            CommandAction.BLOCK_APP,
            CommandAction.SCHEDULE_BLOCK,
            CommandAction.ACTIVATE_MODE
        ) || cmd.trigger == CommandTrigger.ON_APP_OPEN

        val needsOverlay = cmd.action in listOf(
            CommandAction.BLOCK_APP,
            CommandAction.SCHEDULE_BLOCK,
            CommandAction.ACTIVATE_MODE
        )

        val needsNotification = cmd.action in listOf(
            CommandAction.MUTE_NOTIFICATIONS,
            CommandAction.UNMUTE_NOTIFICATIONS,
            CommandAction.ACTIVATE_MODE
        )

        val needsUsage = cmd.action == CommandAction.SET_TIME_LIMIT

        // Check in priority order — return the first missing one
        if (needsAccessibility && !PermissionHelper.hasAccessibilityService(app)) {
            return PermissionRequest(
                type           = RequiredPermissionType.ACCESSIBILITY,
                title          = "Accessibility Service Required",
                reason         = actionReason(cmd.action, RequiredPermissionType.ACCESSIBILITY),
                pendingCommand = rawInput
            )
        }
        if (needsOverlay && !PermissionHelper.hasOverlayPermission(app)) {
            return PermissionRequest(
                type           = RequiredPermissionType.OVERLAY,
                title          = "Overlay Permission Required",
                reason         = actionReason(cmd.action, RequiredPermissionType.OVERLAY),
                pendingCommand = rawInput
            )
        }
        if (needsNotification && !PermissionHelper.hasNotificationAccess(app)) {
            return PermissionRequest(
                type           = RequiredPermissionType.NOTIFICATION_ACCESS,
                title          = "Notification Access Required",
                reason         = actionReason(cmd.action, RequiredPermissionType.NOTIFICATION_ACCESS),
                pendingCommand = rawInput
            )
        }
        if (needsUsage && !PermissionHelper.hasUsageAccess(app)) {
            return PermissionRequest(
                type           = RequiredPermissionType.USAGE_ACCESS,
                title          = "Usage Access Required",
                reason         = actionReason(cmd.action, RequiredPermissionType.USAGE_ACCESS),
                pendingCommand = rawInput
            )
        }
        return null
    }

    private fun actionReason(action: CommandAction, perm: RequiredPermissionType): String =
        when (perm) {
            RequiredPermissionType.ACCESSIBILITY -> when (action) {
                CommandAction.BLOCK_APP, CommandAction.SCHEDULE_BLOCK ->
                    "TaskPulse needs Accessibility Service to detect when a blocked app is opened and show the block screen."
                CommandAction.ACTIVATE_MODE ->
                    "TaskPulse needs Accessibility Service to enforce focus mode by blocking apps when they're opened."
                else ->
                    "TaskPulse needs Accessibility Service to monitor and control app launches."
            }
            RequiredPermissionType.OVERLAY -> when (action) {
                CommandAction.BLOCK_APP, CommandAction.SCHEDULE_BLOCK ->
                    "TaskPulse needs Overlay Permission to show a fullscreen block screen when you try to open a blocked app."
                CommandAction.ACTIVATE_MODE ->
                    "TaskPulse needs Overlay Permission to display the focus mode overlay when you open a restricted app."
                else ->
                    "TaskPulse needs Overlay Permission to display block screens over other apps."
            }
            RequiredPermissionType.NOTIFICATION_ACCESS ->
                "TaskPulse needs Notification Access to intercept and silence notifications from apps during mute sessions."
            RequiredPermissionType.USAGE_ACCESS ->
                "TaskPulse needs Usage Access to track how long you spend in each app and enforce daily time limits."
        }
}
