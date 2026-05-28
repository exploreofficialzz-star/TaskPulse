package com.chastechgroup.taskpulse.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.chastechgroup.taskpulse.engine.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskPulseNotificationListener : NotificationListenerService() {

    private lateinit var ruleEngine: RuleEngine
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        var instance: TaskPulseNotificationListener? = null
        fun isRunning() = instance != null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        ruleEngine = RuleEngine(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return

        scope.launch {
            if (ruleEngine.areNotificationsMuted(pkg)) {
                try {
                    cancelNotification(sbn.key)
                } catch (e: Exception) {
                    // ignore – we may not always have permission at exact moment
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) { /* no-op */ }
    override fun onListenerDisconnected() { instance = null }
    override fun onDestroy() { super.onDestroy(); instance = null }
}
