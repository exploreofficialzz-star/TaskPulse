package com.chastechgroup.taskpulse.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastechgroup.taskpulse.data.models.*
import com.chastechgroup.taskpulse.engine.CommandParser
import com.chastechgroup.taskpulse.ui.theme.*
import com.chastechgroup.taskpulse.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRules: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Points low warning
    val pointsLow = uiState.points < 10

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "TaskPulse",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Points chip
                    PointsChip(points = uiState.points, low = pointsLow, onClick = onNavigateToStore)
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onNavigateToPermissions) {
                        Icon(Icons.Outlined.Security, "Permissions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                onHome = {},
                onRules = onNavigateToRules,
                onApps = onNavigateToApps,
                onStore = onNavigateToStore,
                currentRoute = "home"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Command input card ────────────────────────────────────
            item {
                CommandInputCard(
                    input = uiState.commandInput,
                    onInputChange = viewModel::onCommandChanged,
                    onRun = {
                        focusManager.clearFocus()
                        viewModel.runCommand()
                    },
                    isLoading = uiState.isLoading,
                    preview = uiState.parsedPreview,
                    isDark = isDark
                )
            }

            // ── Result toast ──────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = uiState.lastResult.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ResultBanner(
                        message = uiState.lastResult,
                        isSuccess = uiState.isSuccess,
                        onDismiss = viewModel::clearResult
                    )
                }
            }

            // ── Quick Modes ───────────────────────────────────────────
            item {
                QuickModesRow(
                    onModeSelected = { mode, hours ->
                        viewModel.activateQuickMode(mode, hours)
                    }
                )
            }

            // ── Preset commands ───────────────────────────────────────
            item {
                PresetCommandsRow(
                    onPresetSelected = { preset ->
                        viewModel.onCommandChanged(preset)
                    }
                )
            }

            // ── Active automations ────────────────────────────────────
            if (uiState.activeRules.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Active Automations",
                        count = uiState.activeRules.size,
                        onSeeAll = onNavigateToRules
                    )
                }
                items(uiState.activeRules.take(4)) { rule ->
                    ActiveRuleCard(
                        rule = rule,
                        onToggle = { viewModel.toggleRule(rule.id, !rule.isActive) },
                        onDelete = { viewModel.deleteRule(rule.id) }
                    )
                }
            } else {
                item { EmptyRulesHint() }
            }
        }

        // Points alert dialog
        if (uiState.showPointsAlert) {
            PointsAlertDialog(
                points = uiState.points,
                onDismiss = viewModel::dismissPointsAlert,
                onBuyPoints = { viewModel.dismissPointsAlert(); onNavigateToStore() }
            )
        }

        // Permission gate dialog — shown when a command needs a missing permission
        uiState.permissionRequest?.let { request ->
            PermissionRequestDialog(
                request   = request,
                onDismiss = viewModel::dismissPermissionRequest,
                onRecheck = viewModel::recheckPermissions
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMMAND INPUT CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CommandInputCard(
    input: String,
    onInputChange: (String) -> Unit,
    onRun: () -> Unit,
    isLoading: Boolean,
    preview: ParsedCommand?,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        0.3f, 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Blue500.copy(alpha = if (input.isNotEmpty()) glowAlpha else 0.3f),
                            Cyan400.copy(alpha = if (input.isNotEmpty()) glowAlpha * 0.8f else 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "What do you want TaskPulse to do?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "e.g. Block Instagram for 2 hours",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onRun() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Blue500
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Preview chip
                AnimatedVisibility(visible = preview != null && preview.isValid) {
                    preview?.let {
                        CommandPreviewChip(cmd = it)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // RUN button
                Button(
                    onClick = onRun,
                    enabled = input.isNotEmpty() && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue600,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "RUN",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandPreviewChip(cmd: ParsedCommand) {
    val color = when (cmd.action) {
        CommandAction.BLOCK_APP -> Red500
        CommandAction.MUTE_NOTIFICATIONS -> Orange500
        CommandAction.ACTIVATE_MODE -> Blue500
        CommandAction.UNBLOCK_APP -> Green500
        else -> Blue400
    }
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text = CommandParser.summarize(cmd),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        val cost = CommandParser.calculatePointsCost(cmd)
        Text(
            text = "−$cost pts",
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUICK MODES
// ─────────────────────────────────────────────────────────────────────────────
private data class QuickMode(
    val label: String,
    val mode: FocusMode,
    val icon: ImageVector,
    val color: Color,
    val hours: Int
)

@Composable
private fun QuickModesRow(onModeSelected: (FocusMode, Int) -> Unit) {
    val modes = listOf(
        QuickMode("Focus", FocusMode.FOCUS, Icons.Default.CenterFocusStrong, Blue500, 1),
        QuickMode("Study", FocusMode.STUDY, Icons.Default.MenuBook, Purple500, 2),
        QuickMode("Work",  FocusMode.WORK,  Icons.Default.Work, Green500, 3),
        QuickMode("Sleep", FocusMode.SLEEP, Icons.Default.Bedtime, Cyan400, 8)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel("Quick Modes")
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(modes) { qm ->
                QuickModeChip(qm) { onModeSelected(qm.mode, qm.hours) }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun QuickModeChip(qm: QuickMode, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(qm.color.copy(alpha = 0.15f), qm.color.copy(alpha = 0.07f))
                )
            )
            .border(1.dp, qm.color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(qm.icon, null, tint = qm.color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(qm.label, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text("${qm.hours}h", style = MaterialTheme.typography.labelSmall,
            color = qm.color.copy(alpha = 0.7f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRESET COMMANDS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PresetCommandsRow(onPresetSelected: (String) -> Unit) {
    val presets = listOf(
        "Mute all social apps for 1 hour",
        "Block Instagram for 2 hours",
        "Study mode for 3 hours",
        "Don't show notifications from TikTok until 6pm",
        "When I open Facebook, block it after 10 minutes for 2 hours",
        "Sleep mode for 8 hours"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel("Quick Presets")
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onPresetSelected(preset) },
                    label = {
                        Text(
                            preset,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(14.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = Blue400
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        enabled = true, selected = false
                    )
                )
            }
        }
    }
    Spacer(Modifier.height(20.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVE RULE CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActiveRuleCard(
    rule: AutomationRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val actionColor = when (rule.action) {
        CommandAction.BLOCK_APP -> Red500
        CommandAction.MUTE_NOTIFICATIONS -> Orange500
        CommandAction.ACTIVATE_MODE -> Blue500
        else -> Green500
    }
    val actionIcon = when (rule.action) {
        CommandAction.BLOCK_APP -> Icons.Default.Block
        CommandAction.MUTE_NOTIFICATIONS -> Icons.Default.NotificationsOff
        CommandAction.ACTIVATE_MODE -> Icons.Default.CenterFocusStrong
        else -> Icons.Default.CheckCircle
    }

    val timeLeft = remember(rule.expiresAt) {
        if (rule.expiresAt > System.currentTimeMillis()) {
            val diff = rule.expiresAt - System.currentTimeMillis()
            CommandParser.formatDuration(diff / 1000)
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(actionColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(actionIcon, null, tint = actionColor, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (timeLeft.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Timer, null,
                            modifier = Modifier.size(12.dp),
                            tint = actionColor.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "$timeLeft left",
                            style = MaterialTheme.typography.labelSmall,
                            color = actionColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Toggle
            Switch(
                checked = rule.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Blue600
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline, "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PointsChip(points: Int, low: Boolean, onClick: () -> Unit) {
    val color = if (low) Orange500 else Blue500
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Stars, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "$points pts",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResultBanner(message: String, isSuccess: Boolean, onDismiss: () -> Unit) {
    val color = if (isSuccess) Green500 else Red500
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                null, tint = color, modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = color,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, null, tint = color, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SectionHeader(title: String, count: Int, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (count > 4) {
            TextButton(onClick = onSeeAll) {
                Text("See all ($count)", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EmptyRulesHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.AutoAwesome, null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No active automations",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Type a command above to get started",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PointsAlertDialog(
    points: Int,
    onDismiss: () -> Unit,
    onBuyPoints: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Stars, null, tint = Orange500) },
        title = { Text("Not Enough Points") },
        text = {
            Text("You have $points points. Purchase more to continue using automations.",
                style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(onClick = onBuyPoints,
                colors = ButtonDefaults.buttonColors(containerColor = Blue600)) {
                Text("Buy Points")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}
