package com.chastechgroup.taskpulse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastechgroup.taskpulse.data.models.*
import com.chastechgroup.taskpulse.engine.CommandParser
import com.chastechgroup.taskpulse.ui.theme.*
import com.chastechgroup.taskpulse.viewmodel.RulesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(onBack: () -> Unit, viewModel: RulesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val filtered = viewModel.getFilteredRules()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Automations", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    FilterButtons(current = uiState.filterActive, onFilter = viewModel::setFilter)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.RuleFolder, null,
                        tint = MaterialTheme.colorScheme.primary.copy(0.3f),
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No rules found", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        "${filtered.size} rule${if (filtered.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(filtered, key = { it.id }) { rule ->
                    RuleDetailCard(
                        rule = rule,
                        onToggle = { viewModel.toggleRule(rule.id, !rule.isActive) },
                        onDelete = { viewModel.deleteRule(rule.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterButtons(current: Boolean?, onFilter: (Boolean?) -> Unit) {
    Row {
        listOf(null to "All", true to "Active", false to "Paused").forEach { (value, label) ->
            val selected = current == value
            FilterChip(
                selected = selected,
                onClick = { onFilter(value) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.padding(horizontal = 2.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Blue600,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun RuleDetailCard(rule: AutomationRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val actionColor = when (rule.action) {
        CommandAction.BLOCK_APP -> Red500
        CommandAction.MUTE_NOTIFICATIONS -> Orange500
        CommandAction.ACTIVATE_MODE -> Blue500
        CommandAction.UNBLOCK_APP -> Green500
        else -> Blue400
    }
    val actionLabel = when (rule.action) {
        CommandAction.BLOCK_APP -> "Block"
        CommandAction.MUTE_NOTIFICATIONS -> "Mute"
        CommandAction.ACTIVATE_MODE -> rule.mode?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Mode"
        CommandAction.UNBLOCK_APP -> "Unblock"
        else -> "Rule"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (rule.isActive) 1.5.dp else 1.dp,
            if (rule.isActive) actionColor.copy(0.4f) else MaterialTheme.colorScheme.outline.copy(0.3f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionColor.copy(0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall,
                        color = actionColor, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                if (!rule.isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("PAUSED", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = rule.isActive, onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Blue600)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(rule.name, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)

            if (rule.description.isNotEmpty() && rule.description != rule.name) {
                Text(
                    "\"${rule.description}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Stats row
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (rule.durationSeconds > 0) {
                    StatChip(Icons.Outlined.Timer,
                        CommandParser.formatDuration(rule.durationSeconds), Blue400)
                }
                StatChip(Icons.Outlined.Stars, "${rule.pointsCost} pts", Orange500)
                if (rule.targetApps.isNotEmpty()) {
                    StatChip(Icons.Outlined.Apps, "${rule.targetApps.size} apps", Purple500)
                }
            }

            // Expand / delete
            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Less" else "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Outlined.DeleteOutline, "Delete",
                        tint = Red500.copy(0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rule?") },
            text = { Text("This will permanently remove '${rule.name}'.") },
            confirmButton = {
                Button(onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
