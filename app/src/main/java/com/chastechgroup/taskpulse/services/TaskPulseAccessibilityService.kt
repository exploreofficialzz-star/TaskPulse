package com.chastechgroup.taskpulse.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.chastechgroup.taskpulse.data.db.AppDatabase
import com.chastechgroup.taskpulse.engine.RuleEngine
import com.chastechgroup.taskpulse.ui.screens.BlockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskPulseAccessibilityService : AccessibilityService() {

    private lateinit var ruleEngine: RuleEngine
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastBlockedPkg = ""
    private var lastEventTime  = 0L

    companion object {
        var instance: TaskPulseAccessibilityService? = null
        fun isRunning() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        ruleEngine = RuleEngine(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastEventTime < 2000) return
        lastEventTime = now

        scope.launch {
            if (ruleEngine.isAppBlocked(pkg)) {
                lastBlockedPkg = pkg
                val details = ruleEngine.getBlockDetails(pkg)
                showBlockOverlay(pkg, details)
            }
        }
    }

    private fun showBlockOverlay(
        pkg: String,
        details: RuleEngine.BlockDetails?
    ) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("blocked_package", pkg)
            putExtra("app_name",        details?.appName      ?: friendlyAppName(pkg))
            putExtra("expires_at",      details?.expiresAt    ?: 0L)
            putExtra("rule_name",       details?.ruleName     ?: "")
            putExtra("block_reason",    details?.blockReason  ?: "")
        }
        startActivity(intent)
    }

    private fun friendlyAppName(pkg: String): String =
        pkg.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "This App"

    override fun onInterrupt() { instance = null }
    override fun onDestroy()   { super.onDestroy(); instance = null }
}
