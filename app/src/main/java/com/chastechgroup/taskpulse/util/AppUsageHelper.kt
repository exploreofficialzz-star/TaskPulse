package com.chastechgroup.taskpulse.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.chastechgroup.taskpulse.data.entities.AppInfoEntity

object AppUsageHelper {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getForegroundApp(context: Context): String? {
        if (!hasUsageAccess(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 5000, now)
        return stats?.filter { it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    fun getDailyUsage(context: Context, packageName: String): Long {
        if (!hasUsageAccess(context)) return 0L
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis()
        )
        return stats?.firstOrNull { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    fun getAllInstalledApps(context: Context): List<AppInfoEntity> {
        val pm = context.packageManager
        val categorizedPkgs = getDefaultCategories()
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { info ->
                val name = pm.getApplicationLabel(info).toString()
                val pkg = info.packageName
                AppInfoEntity(
                    packageName = pkg,
                    appName = name,
                    category = categorizedPkgs[pkg] ?: "other",
                    isInstalled = true
                )
            }
    }

    private fun getDefaultCategories(): Map<String, String> = mapOf(
        "com.instagram.android" to "social",
        "com.facebook.katana" to "social",
        "com.zhiliaoapp.musically" to "social",
        "com.twitter.android" to "social",
        "com.snapchat.android" to "social",
        "com.reddit.frontpage" to "social",
        "com.pinterest" to "social",
        "com.linkedin.android" to "social",
        "com.discord" to "social",
        "com.google.android.youtube" to "entertainment",
        "com.netflix.mediaclient" to "entertainment",
        "tv.twitch.android.app" to "entertainment",
        "com.spotify.music" to "entertainment",
        "com.android.chrome" to "productivity",
        "com.google.android.gm" to "productivity",
        "com.whatsapp" to "communication",
        "org.telegram.messenger" to "communication",
        "com.google.android.apps.messaging" to "communication"
    )
}
