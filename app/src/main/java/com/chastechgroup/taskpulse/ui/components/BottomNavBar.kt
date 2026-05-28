package com.chastechgroup.taskpulse.ui.screens

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chastechgroup.taskpulse.ui.theme.Blue600

private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navItems = listOf(
    NavItem("home",  "Home",  Icons.Filled.Home,         Icons.Outlined.Home),
    NavItem("rules", "Rules", Icons.Filled.Rule,          Icons.Outlined.Rule),
    NavItem("apps",  "Apps",  Icons.Filled.Apps,          Icons.Outlined.Apps),
    NavItem("store", "Store", Icons.Filled.Stars,          Icons.Outlined.Stars)
)

@Composable
fun BottomNavBar(
    onHome: () -> Unit,
    onRules: () -> Unit,
    onApps: () -> Unit,
    onStore: () -> Unit,
    currentRoute: String
) {
    val actions = mapOf("home" to onHome, "rules" to onRules, "apps" to onApps, "store" to onStore)
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.height(72.dp)
    ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { actions[item.route]?.invoke() },
                icon = {
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        item.label
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Blue600,
                    selectedTextColor = Blue600,
                    indicatorColor = Blue600.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
