package com.chastechgroup.taskpulse.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastechgroup.taskpulse.ui.theme.*
import com.chastechgroup.taskpulse.viewmodel.PointPackage
import com.chastechgroup.taskpulse.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onBack: () -> Unit, viewModel: StoreViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? android.app.Activity

    LaunchedEffect(uiState.purchaseResult) {
        if (uiState.purchaseResult.isNotEmpty()) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Points Store", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Balance card ───────────────────────────────────────────
            item { BalanceCard(points = uiState.currentPoints) }

            // ── Result message ─────────────────────────────────────────
            if (uiState.purchaseResult.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Green500.copy(0.12f))
                            .padding(14.dp)
                    ) {
                        Text(uiState.purchaseResult, color = Green500,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Free tier banner ───────────────────────────────────────
            item { FreeTierBanner() }

            // ── Packages ───────────────────────────────────────────────
            item {
                Text("Point Packages",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            items(uiState.packages) { pkg ->
                PointPackageCard(
                    pkg = pkg,
                    isLoading = uiState.isLoading,
                    onPurchase = {
                        activity?.let { viewModel.purchasePackage(it, pkg) }
                    }
                )
            }

            // ── Usage info ─────────────────────────────────────────────
            item { PointsUsageInfo() }
        }
    }
}

@Composable
private fun BalanceCard(points: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Navy700, Blue600.copy(alpha = 0.8f))
                )
            )
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Stars, null, tint = Yellow500, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text("Your Balance", style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(0.7f), letterSpacing = 1.sp)
            Text("$points", style = MaterialTheme.typography.displayLarge,
                color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("points", style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.7f))
        }
    }
}

@Composable
private fun FreeTierBanner() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Green500.copy(0.08f))
            .border(1.dp, Green500.copy(0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CardGiftcard, null, tint = Green500, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("50 Free Points on Install!", style = MaterialTheme.typography.titleMedium,
                color = Green500, fontWeight = FontWeight.Bold)
            Text("New users get 50 points to try all features",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PointPackageCard(pkg: PointPackage, isLoading: Boolean, onPurchase: () -> Unit) {
    val popular = pkg.id == "p500"
    val gradient = when (pkg.id) {
        "p100"  -> listOf(Blue600.copy(0.15f), Blue500.copy(0.05f))
        "p500"  -> listOf(Purple500.copy(0.2f), Blue600.copy(0.1f))
        "p1000" -> listOf(Yellow500.copy(0.15f), Orange500.copy(0.07f))
        else    -> listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
    }
    val accentColor = when (pkg.id) {
        "p100"  -> Blue500
        "p500"  -> Purple500
        "p1000" -> Yellow500
        else    -> Blue500
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(
                if (popular) 2.dp else 1.dp,
                if (popular) Purple500.copy(0.6f) else MaterialTheme.colorScheme.outline.copy(0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(gradient))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${pkg.points} Points",
                            style = MaterialTheme.typography.headlineMedium,
                            color = accentColor, fontWeight = FontWeight.ExtraBold)
                        Text("${pkg.price} USD",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            when (pkg.id) {
                                "p100"  -> "Great for trying features"
                                "p500"  -> "Best value — most popular"
                                "p1000" -> "Power user bundle"
                                else    -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onPurchase,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Buy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (popular) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(y = (-8).dp, x = (-12).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Purple500)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("POPULAR", style = MaterialTheme.typography.labelSmall,
                    color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun PointsUsageInfo() {
    val items = listOf(
        "Creating an automation rule" to "5 pts",
        "Running a focus session (1h)" to "5–15 pts",
        "Muting notifications" to "3 pts",
        "Time-based app blocking (per hour)" to "5 pts"
    )
    Column(modifier = Modifier.padding(16.dp)) {
        Text("How Points Are Used", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        items.forEach { (label, cost) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                Text(cost, style = MaterialTheme.typography.labelMedium,
                    color = Blue400, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))
        }
    }
}
