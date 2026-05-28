package com.chastechgroup.taskpulse.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chastechgroup.taskpulse.data.entities.AppInfoEntity
import com.chastechgroup.taskpulse.data.repository.TaskPulseRepository
import com.chastechgroup.taskpulse.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { TaskPulseRepository(context) }
    val scope = rememberCoroutineScope()

    val allApps by repo.getAllApps().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }

    val categories = listOf("all", "social", "entertainment", "gaming", "productivity", "communication", "other")
    val filtered = allApps
        .filter { if (selectedCategory == "all") true else it.category == selectedCategory }
        .filter { it.appName.contains(searchQuery, true) || it.packageName.contains(searchQuery, true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Manager", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search apps…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) { {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null)
                    }
                } } else null,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue500,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.3f)
                ),
                singleLine = true
            )

            // Category filter
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                edgePadding = 16.dp,
                divider = {},
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (allApps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Blue500)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading apps…", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        Text("${filtered.size} apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                    items(filtered, key = { it.packageName }) { app ->
                        AppListItem(app = app, onCategoryChange = { newCat ->
                            scope.launch { repo.updateAppCategory(app.packageName, newCat) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(app: AppInfoEntity, onCategoryChange: (String) -> Unit) {
    val catColor = when (app.category) {
        "social" -> Blue500
        "entertainment" -> Purple500
        "gaming" -> Green500
        "productivity" -> Orange500
        "communication" -> Cyan400
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    var showCatMenu by remember { mutableStateOf(false) }
    val categories = listOf("social", "entertainment", "gaming", "productivity", "communication", "other")

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon placeholder
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(catColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(app.appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = catColor, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.appName, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1)
        }
        Box {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(catColor.copy(0.12f))
                    .clickable { showCatMenu = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(app.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall, color = catColor)
            }
            DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.replaceFirstChar { it.uppercase() }) },
                        onClick = { onCategoryChange(cat); showCatMenu = false }
                    )
                }
            }
        }
    }
}
