package com.chastechgroup.taskpulse.ui.screens

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.chastechgroup.taskpulse.ui.theme.*
import com.chastechgroup.taskpulse.ui.theme.TaskPulseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlockOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val pkg         = intent.getStringExtra("blocked_package") ?: ""
        val appName     = intent.getStringExtra("app_name")
            ?: pkg.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "This App"
        val expiresAt   = intent.getLongExtra("expires_at", 0L)
        val ruleName    = intent.getStringExtra("rule_name") ?: ""
        val blockReason = intent.getStringExtra("block_reason") ?: ""
        val repo        = TaskPulseRepository(applicationContext)

        setContent {
            TaskPulseTheme {
                BlockOverlayScreen(
                    packageName = pkg,
                    appName     = appName,
                    expiresAt   = expiresAt,
                    ruleName    = ruleName,
                    blockReason = blockReason,
                    onGoHome = {
                        startActivity(
                            android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_HOME)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                        finish()
                    },
                    onUnblock = {
                        val scope = kotlinx.coroutines.MainScope()
                        scope.launch {
                            repo.removeBlock(pkg)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  App-specific friendly messages
// ─────────────────────────────────────────────────────────────────────────────
private val APP_MESSAGES = mapOf(
    "com.instagram.android" to listOf(
        "The highlight reel can wait.\nYour real life is happening now.",
        "Everyone looks better with a filter.\nYour focus looks better without distractions.",
        "You've scrolled enough today.\nLet your mind breathe.",
        "Real moments > curated ones.\nGet back to yours.",
        "The gram will still be there.\nYour goals need you now."
    ),
    "com.facebook.katana" to listOf(
        "The news feed can wait.\nYour focus can't.",
        "Real connections happen offline.\nGo make one.",
        "You blocked this yourself.\nPast you was smart. Trust them.",
        "More doing, less scrolling.\nYou've got this.",
        "The world isn't ending on Facebook.\nBut your productivity might be if you keep scrolling."
    ),
    "com.zhiliaoapp.musically" to listOf(
        "One more video becomes one more hour.\nYou know this.",
        "The algorithm is designed to keep you here.\nYou're stronger than it.",
        "You just reclaimed 30 minutes of your life.\nUse them well.",
        "That video will still exist later.\nYour focus window won't.",
        "TikTok is waiting.\nSo is your potential."
    ),
    "com.twitter.android" to listOf(
        "The discourse will survive without you.\nFor now.",
        "Outrage and hot takes can wait.\nYour peace of mind matters more.",
        "The timeline moves fast.\nSo does your life. Don't miss it.",
        "You set this block for a reason.\nHonor that decision.",
        "Less tweets, more doing.\nBack to work."
    ),
    "com.google.android.youtube" to listOf(
        "One more video = one more rabbit hole.\nYou know how this ends.",
        "Content will always be there.\nYour focus time is limited.",
        "The creator can wait.\nYour goals are calling louder.",
        "You unlocked your screen time.\nNow unlock your potential.",
        "Autoplay is off right now.\nYour momentum is on."
    ),
    "com.whatsapp" to listOf(
        "They know you're focused right now.\nThey'll understand.",
        "The group chat is fine without you.\nFor a bit.",
        "Deep work requires deep focus.\nReplies can wait.",
        "You'll respond better after your session.\nStay locked in.",
        "The messages will be there.\nSo will the people who matter."
    ),
    "com.snapchat.android" to listOf(
        "Your streak isn't worth your focus session.",
        "Real moments > ephemeral snaps.\nBe present.",
        "24 hours is a long time for a snap.\nYou've got time later.",
        "Less snapping, more doing.\nYou set this block yourself.",
        "Your story can wait.\nYour goals can't."
    ),
    "com.reddit.frontpage" to listOf(
        "The front page will still be weird later.\nStay focused.",
        "One subreddit leads to another.\nYou know the drill.",
        "The internet's opinions can wait.\nYours are what matter right now.",
        "Less browsing, more building.\nYou've got work to do.",
        "Reddit will be fine without your upvotes for now."
    )
)

private val GENERAL_MESSAGES = listOf(
    "You set this rule yourself.\nTrust your past self — they were right.",
    "Every time you resist, you\nget stronger. This is one of those times.",
    "What you focus on, you become.\nChoose wisely.",
    "Your future self is watching.\nDon't let them down.",
    "Small disciplines compound into\nextraordinary results.",
    "This is your focused time.\nOwn every minute of it.",
    "Distraction is the enemy of greatness.\nYou're blocking it like a pro.",
    "The best investment you can make\nis in your own focus.",
    "You didn't block this app by accident.\nYou had a vision. Keep it.",
    "Champions focus when it's hard.\nYou're being a champion right now."
)

private val ALTERNATIVES = listOf(
    "💧 Drink a glass of water",
    "🚶 Take a 5-min walk",
    "📝 Write down one thing you're grateful for",
    "🧘 Take 5 deep breaths",
    "✅ Cross off one item on your to-do list",
    "📖 Read one page of a book",
    "🙆 Do a quick stretch",
    "☕ Make yourself a hot drink"
)

// ─────────────────────────────────────────────────────────────────────────────
//  Overlay Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BlockOverlayScreen(
    packageName: String,
    appName: String,
    expiresAt: Long,
    ruleName: String,
    blockReason: String,
    onGoHome: () -> Unit,
    onUnblock: () -> Unit
) {
    // ── Animations ─────────────────────────────────────────────────────
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )

    // ── Message rotation ────────────────────────────────────────────────
    val messages = APP_MESSAGES[packageName] ?: GENERAL_MESSAGES
    var msgIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            msgIndex = (msgIndex + 1) % messages.size
        }
    }

    // ── Alternative suggestion ──────────────────────────────────────────
    val alternative = remember { ALTERNATIVES.random() }

    // ── Countdown timer ─────────────────────────────────────────────────
    var timeLeft by remember { mutableStateOf("") }
    LaunchedEffect(expiresAt) {
        while (true) {
            val remaining = expiresAt - System.currentTimeMillis()
            timeLeft = if (expiresAt > 0L && remaining > 0L) {
                val h = remaining / 3_600_000L
                val m = (remaining % 3_600_000L) / 60_000L
                val s = (remaining % 60_000L) / 1_000L
                when {
                    h > 0 -> "${h}h ${m}m remaining"
                    m > 0 -> "${m}m ${s}s remaining"
                    else  -> "${s}s remaining"
                }
            } else ""
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050A1A), Color(0xFF0A0F1F), Color(0xFF060B18))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Pulsing lock icon ───────────────────────────────────────
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                Box(
                    Modifier.size(140.dp).alpha(pulse * 0.15f).background(
                        Brush.radialGradient(listOf(Blue500, Color.Transparent)),
                        CircleShape
                    )
                )
                Box(
                    Modifier.size(88.dp).clip(CircleShape)
                        .background(Blue600.copy(0.18f))
                        .border(1.5.dp, Blue500.copy(0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = Blue400, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── App name ────────────────────────────────────────────────
            Text(
                text       = appName,
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )

            Spacer(Modifier.height(4.dp))

            // ── Rule label ──────────────────────────────────────────────
            if (ruleName.isNotBlank()) {
                Text(
                    text  = ruleName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Blue400,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
            }

            // ── Countdown ───────────────────────────────────────────────
            if (timeLeft.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Timer, null,
                        tint     = Color.White.copy(0.45f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text  = timeLeft,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.45f)
                    )
                }
                Spacer(Modifier.height(28.dp))
            } else {
                Spacer(Modifier.height(20.dp))
            }

            // ── Rotating motivational message ───────────────────────────
            AnimatedContent(
                targetState = msgIndex,
                transitionSpec = {
                    (fadeIn(tween(600)) + slideInVertically { it / 3 })
                        .togetherWith(fadeOut(tween(400)) + slideOutVertically { -it / 3 })
                },
                label = "message"
            ) { idx ->
                Text(
                    text       = messages[idx],
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color.White.copy(0.85f),
                    textAlign  = TextAlign.Center,
                    lineHeight = 26.sp,
                    modifier   = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Alternative suggestion ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(0.05f))
                    .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        "Instead, try:",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = Color.White.copy(0.4f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        alternative,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.85f)
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Go Home button ──────────────────────────────────────────
            Button(
                onClick  = onGoHome,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                Icon(Icons.Default.Home, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Go to Home Screen",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Emergency unblock ───────────────────────────────────────
            OutlinedButton(
                onClick  = onUnblock,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(16.dp),
                border   = BorderStroke(1.dp, Color.White.copy(0.15f)),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(0.35f)
                )
            ) {
                Icon(Icons.Default.LockOpen, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Remove Block", fontSize = 14.sp)
            }
        }
    }
}
