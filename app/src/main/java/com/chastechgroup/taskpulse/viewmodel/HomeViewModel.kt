package com.chastechgroup.taskpulse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chastechgroup.taskpulse.data.models.*
import com.chastechgroup.taskpulse.data.repository.TaskPulseRepository
import com.chastechgroup.taskpulse.engine.CommandParser
import com.chastechgroup.taskpulse.engine.RuleEngine
import com.chastechgroup.taskpulse.util.PreferencesHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val commandInput: String = "",
    val parsedPreview: ParsedCommand? = null,
    val lastResult: String = "",
    val isSuccess: Boolean = false,
    val activeRules: List<AutomationRule> = emptyList(),
    val points: Int = 50,
    val showPointsAlert: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TaskPulseRepository(application)
    private val ruleEngine = RuleEngine(application)
    private val _uiState = MutableStateFlow(HomeUiState())
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

        val cost = CommandParser.calculatePointsCost(cmd)
        val currentPoints = _uiState.value.points

        if (currentPoints < cost) {
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
                            isLoading = false,
                            lastResult = message,
                            isSuccess = success,
                            commandInput = if (success) "" else it.commandInput,
                            parsedPreview = null
                        )
                    }
                }
            }
        }
    }

    fun dismissPointsAlert() = _uiState.update { it.copy(showPointsAlert = false) }

    fun clearResult() = _uiState.update { it.copy(lastResult = "") }

    fun deleteRule(id: Long) = viewModelScope.launch { repo.deleteRule(id) }

    fun toggleRule(id: Long, active: Boolean) =
        viewModelScope.launch { repo.toggleRule(id, active) }

    fun activateQuickMode(mode: FocusMode, durationHours: Int = 1) {
        val input = "${mode.name.lowercase()} mode for $durationHours hour${if (durationHours > 1) "s" else ""}"
        _uiState.update { it.copy(commandInput = input) }
        runCommand()
    }
}
