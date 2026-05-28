package com.chastechgroup.taskpulse.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taskpulse_prefs")

object PreferencesHelper {
    val POINTS_KEY = intPreferencesKey("user_points")
    val ONBOARDED_KEY = booleanPreferencesKey("is_onboarded")
    val TOTAL_COMMANDS_KEY = intPreferencesKey("total_commands")
    val MONITORING_ACTIVE_KEY = booleanPreferencesKey("monitoring_active")
    val THEME_KEY = stringPreferencesKey("app_theme")

    fun getPoints(context: Context): Flow<Int> =
        context.dataStore.data.map { it[POINTS_KEY] ?: 50 }

    suspend fun setPoints(context: Context, points: Int) {
        context.dataStore.edit { it[POINTS_KEY] = points }
    }

    suspend fun deductPoints(context: Context, amount: Int): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val current = prefs[POINTS_KEY] ?: 50
            if (current >= amount) {
                prefs[POINTS_KEY] = current - amount
                success = true
            }
        }
        return success
    }

    suspend fun addPoints(context: Context, amount: Int) {
        context.dataStore.edit { prefs ->
            prefs[POINTS_KEY] = (prefs[POINTS_KEY] ?: 50) + amount
        }
    }

    fun isOnboarded(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDED_KEY] ?: false }

    suspend fun setOnboarded(context: Context) {
        context.dataStore.edit { it[ONBOARDED_KEY] = true }
    }

    fun isMonitoringActive(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[MONITORING_ACTIVE_KEY] ?: false }

    suspend fun setMonitoringActive(context: Context, active: Boolean) {
        context.dataStore.edit { it[MONITORING_ACTIVE_KEY] = active }
    }

    suspend fun incrementCommands(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[TOTAL_COMMANDS_KEY] = (prefs[TOTAL_COMMANDS_KEY] ?: 0) + 1
        }
    }
}
