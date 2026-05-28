package com.chastechgroup.taskpulse.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.chastechgroup.taskpulse.services.TaskPulseAccessibilityService
import com.chastechgroup.taskpulse.services.TaskPulseNotificationListener

object PermissionHelper {

    fun hasUsageAccess(context: Context): Boolean =
        AppUsageHelper.hasUsageAccess(context)

    fun hasAccessibilityService(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )
        val myComponent = ComponentName(context, TaskPulseAccessibilityService::class.java)
        return enabledServices.any { it.resolveInfo.serviceInfo.let { si ->
            si.packageName == myComponent.packageName && si.name == myComponent.className
        }}
    }

    fun hasNotificationAccess(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val myComponent = ComponentName(context, TaskPulseNotificationListener::class.java)
        return flat.split(":").any { it == myComponent.flattenToString() }
    }

    fun hasOverlayPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context)
        else true

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun getAllPermissionsStatus(context: Context): Map<String, Boolean> = mapOf(
        "Usage Access" to hasUsageAccess(context),
        "Accessibility" to hasAccessibilityService(context),
        "Notification Access" to hasNotificationAccess(context),
        "Overlay" to hasOverlayPermission(context)
    )

    fun areAllPermissionsGranted(context: Context): Boolean =
        getAllPermissionsStatus(context).all { it.value }
}
