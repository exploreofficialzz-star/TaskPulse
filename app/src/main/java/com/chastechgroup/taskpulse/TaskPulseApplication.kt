package com.chastechgroup.taskpulse

import android.app.Application
import com.chastechgroup.taskpulse.data.db.AppDatabase
import com.chastechgroup.taskpulse.util.AppUsageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskPulseApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Pre-warm database
        val db = AppDatabase.getInstance(this)
        // Seed installed apps on first run
        scope.launch {
            try {
                val installedApps = AppUsageHelper.getAllInstalledApps(this@TaskPulseApplication)
                db.appInfoDao().insertApps(installedApps)
            } catch (e: Exception) {
                // Silent fail — apps screen will be empty but won't crash
            }
        }
    }
}
