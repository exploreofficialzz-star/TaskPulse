package com.chastechgroup.taskpulse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chastechgroup.taskpulse.ui.theme.*
import com.chastechgroup.taskpulse.util.PermissionHelper
import com.chastechgroup.taskpulse.viewmodel.PermissionRequest
import com.chastechgroup.taskpulse.viewmodel.RequiredPermissionType

/**
 * Full-screen permission bottom sheet shown when a command needs a missing permission.
 *
 * Usage — add inside your HomeScreen Scaffold (anywhere inside the Box/content):
 *
 *   val permReq = uiState.permissionRequest
 *   if (permReq != null) {
 *       PermissionRequestDialog(
 *           request    = permReq,
 *           onDismiss  = { viewModel.dismissPermissionRequest() },
 *           onRecheck  = { viewModel.recheckPermissions() }
 *       )
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestDialog(
    request: PermissionRequest,
    onDismiss: () -> Unit,
    onRecheck: () -> Unit
) {
    val context = LocalContext.current
    val isDark  = isSystemInDarkTheme()

    // Auto-recheck when user comes back from settings
    var hasNavigatedToSettings by remember { mutableStateOf(false) }

    // Lifecycle-aware recheck — fires when composition resumes from background
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && hasNavigatedToSettings) {
                hasNavigatedToSettings = false
                onRecheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = if (isDark) Navy800 else LightSurface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) BorderDark else LightBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(8.dp))

            // ── Permission icon ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Orange500.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = permissionIcon(request.type),
                    contentDescription = null,
                    tint               = Orange500,
                    modifier           = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Title ────────────────────────────────────────────────────
            Text(
                text       = request.title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = if (isDark) TextOnDark else TextOnLight
            )

            Spacer(Modifier.height(12.dp))

            // ── Reason ───────────────────────────────────────────────────
            Text(
                text  = request.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) TextOnDark70 else TextOnLight70,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // ── Pending command pill ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Blue500.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint   = Blue400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = "\"${request.pendingCommand}\"",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Blue400,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Grant Permission button ──────────────────────────────────
            Button(
                onClick = {
                    hasNavigatedToSettings = true
                    when (request.type) {
                        RequiredPermissionType.ACCESSIBILITY       -> PermissionHelper.openAccessibilitySettings(context)
                        RequiredPermissionType.OVERLAY             -> PermissionHelper.openOverlaySettings(context)
                        RequiredPermissionType.NOTIFICATION_ACCESS -> PermissionHelper.openNotificationListenerSettings(context)
                        RequiredPermissionType.USAGE_ACCESS        -> PermissionHelper.openUsageAccessSettings(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue500)
            ) {
                Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Grant Permission",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Cancel ───────────────────────────────────────────────────
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(
                    "Not now",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDark) TextOnDark50 else TextOnLight50
                )
            }
        }
    }
}

@Composable
private fun permissionIcon(type: RequiredPermissionType): ImageVector = when (type) {
    RequiredPermissionType.ACCESSIBILITY       -> Icons.Default.Accessibility
    RequiredPermissionType.OVERLAY             -> Icons.Default.Layers
    RequiredPermissionType.NOTIFICATION_ACCESS -> Icons.Default.Notifications
    RequiredPermissionType.USAGE_ACCESS        -> Icons.Default.BarChart
}

@Composable
private fun isSystemInDarkTheme() = androidx.compose.foundation.isSystemInDarkTheme()
