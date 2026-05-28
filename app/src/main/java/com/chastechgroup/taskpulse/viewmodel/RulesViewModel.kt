package com.chastechgroup.taskpulse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chastechgroup.taskpulse.data.models.AutomationRule
import com.chastechgroup.taskpulse.data.repository.TaskPulseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RulesUiState(
    val allRules: List<AutomationRule> = emptyList(),
    val filterActive: Boolean? = null
)

class RulesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TaskPulseRepository(application)
    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllRules().collect { rules ->
                _uiState.update { it.copy(allRules = rules) }
            }
        }
    }

    fun deleteRule(id: Long) = viewModelScope.launch { repo.deleteRule(id) }

    fun toggleRule(id: Long, active: Boolean) =
        viewModelScope.launch { repo.toggleRule(id, active) }

    fun setFilter(active: Boolean?) = _uiState.update { it.copy(filterActive = active) }

    fun getFilteredRules(): List<AutomationRule> {
        val state = _uiState.value
        return when (state.filterActive) {
            true -> state.allRules.filter { it.isActive }
            false -> state.allRules.filter { !it.isActive }
            null -> state.allRules
        }
    }
}
