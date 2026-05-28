package com.chastechgroup.taskpulse.ui.screens

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chastechgroup.taskpulse.data.repository.TaskPulseRepository
import com.chastechgroup.taskpulse.ui.theme.TaskPulseTheme
import com.chastechgroup.taskpulse.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlockOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        val repo = TaskPulseRepository(applicationContext)

        setContent {
            TaskPulseTheme {
                BlockOverlayScreen(
                    packageName = blockedPackage,
                    onGoHome = {
                        // Navigate to home screen
                        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                        intent.addCategory(android.content.Intent.CATEGORY_HOME)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    },
                    onUnblock = {
                        val scope = kotlinx.coroutines.MainScope()
                        scope.launch {
                            repo.removeBlock(blockedPackage)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BlockOverlayScreen(
    packageName: String,
    onGoHome: () -> Unit,
    onUnblock: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        0.4f, 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse"
    )

    val appDisplayName = packageName.split(".").lastOrNull()
        ?.replaceFirstChar { it.uppercase() } ?: "This App"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Navy800, Navy950),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Block icon with pulse
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .alpha(pulseAlpha * 0.2f)
                        .background(
                            Brush.radialGradient(listOf(Red500, Color.Transparent)),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Red500.copy(0.15f))
                        .border(2.dp, Red500.copy(0.5f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Block, null, tint = Red500, modifier = Modifier.size(44.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Blocked!",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "$appDisplayName is blocked by TaskPulse",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Stay focused. You're doing great!",
                style = MaterialTheme.typography.bodyMedium,
                color = Blue400,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Go home button
            Button(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Go to Home Screen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Emergency unblock
            OutlinedButton(
                onClick = onUnblock,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.5f))
            ) {
                Text("Remove Block", fontWeight = FontWeight.Medium)
            }
        }
    }
}
