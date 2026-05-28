package com.chastechgroup.taskpulse.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chastechgroup.taskpulse.MainActivity
import com.chastechgroup.taskpulse.R
import com.chastechgroup.taskpulse.data.db.AppDatabase
import com.chastechgroup.taskpulse.util.AppUsageHelper
import kotlinx.coroutines.*

class MonitoringForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase

    companion object {
        const val CHANNEL_ID = "taskpulse_monitoring"
        const val NOTIFICATION_ID = 1001
        private var isRunning = false

        fun start(context: Context) {
            if (isRunning) return
            val intent = Intent(context, MonitoringForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitoringForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning = true
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    // Clean expired blocks & filters
                    val now = System.currentTimeMillis()
                    db.appInfoDao().clearExpiredBlocks(now)
                    db.appInfoDao().clearExpiredFilters(now)

                    // Update usage stats for installed apps
                    val apps = db.appInfoDao().getAllApps()
                    // Flow collect done in background
                } catch (e: Exception) {
                    // swallow monitoring errors
                }
                delay(5_000L) // check every 5 seconds
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TaskPulse Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps TaskPulse running in background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TaskPulse Active")
            .setContentText("Monitoring your automations")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        isRunning = false
    }
}
