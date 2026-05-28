package com.chastechgroup.taskpulse.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chastechgroup.taskpulse.ui.theme.*
import com.chastechgroup.taskpulse.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }

    val perms by remember(refreshTrigger) {
        derivedStateOf {
            mapOf(
                "Usage Access" to PermissionHelper.hasUsageAccess(context),
                "Accessibility Service" to PermissionHelper.hasAccessibilityService(context),
                "Notification Access" to PermissionHelper.hasNotificationAccess(context),
                "Overlay Permission" to PermissionHelper.hasOverlayPermission(context)
            )
        }
    }

    val allGranted = perms.all { it.value }

    // Refresh when screen resumes
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            refreshTrigger++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)) {

            // ── Status summary card ────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (allGranted) Green500.copy(0.12f) else Orange500.copy(0.12f)
                        )
                        .border(
                            1.dp,
                            if (allGranted) Green500.copy(0.4f) else Orange500.copy(0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (allGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            null,
                            tint = if (allGranted) Green500 else Orange500,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (allGranted) "All permissions granted" else "Some permissions missing",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (allGranted) Green500 else Orange500,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (allGranted) "TaskPulse is fully operational"
                                else "Grant all permissions for full functionality",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Permission items ───────────────────────────────────────
            item {
                PermissionItem(
                    icon = Icons.Outlined.BarChart,
                    title = "Usage Access",
                    description = "Allows TaskPulse to monitor which apps you use and for how long. Required for time-limit enforcement.",
                    isGranted = perms["Usage Access"] ?: false,
                    onGrant = { PermissionHelper.openUsageAccessSettings(context) }
                )
            }
            item {
                PermissionItem(
                    icon = Icons.Outlined.Accessibility,
                    title = "Accessibility Service",
                    description = "Enables TaskPulse to detect app launches and show block overlays when rules are triggered.",
                    isGranted = perms["Accessibility Service"] ?: false,
                    onGrant = { PermissionHelper.openAccessibilitySettings(context) }
                )
            }
            item {
                PermissionItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notification Access",
                    description = "Lets TaskPulse intercept and suppress notifications from apps during mute sessions.",
                    isGranted = perms["Notification Access"] ?: false,
                    onGrant = { PermissionHelper.openNotificationListenerSettings(context) }
                )
            }
            item {
                PermissionItem(
                    icon = Icons.Outlined.Layers,
                    title = "Overlay Permission",
                    description = "Allows TaskPulse to show a fullscreen block overlay when you open a blocked app.",
                    isGranted = perms["Overlay Permission"] ?: false,
                    onGrant = { PermissionHelper.openOverlaySettings(context) }
                )
            }

            // ── Privacy note ───────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Lock, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "TaskPulse is fully offline. No data ever leaves your device. " +
                            "All automations and usage data are stored locally only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isGranted) Green500.copy(0.35f) else MaterialTheme.colorScheme.outline.copy(0.3f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isGranted) Green500.copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null,
                    tint = if (isGranted) Green500 else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(
                        if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (isGranted) Green500 else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!isGranted) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onGrant,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Blue500,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, Blue500.copy(0.6f))
                    ) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Grant Permission", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
