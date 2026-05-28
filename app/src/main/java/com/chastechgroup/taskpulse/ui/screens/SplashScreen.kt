package com.chastechgroup.taskpulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chastechgroup.taskpulse.R
import com.chastechgroup.taskpulse.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val isDark = isSystemInDarkTheme()

    // Animations
    val logoScale   = remember { Animatable(0.4f) }
    val logoAlpha   = remember { Animatable(0f) }
    val textAlpha   = remember { Animatable(0f) }
    val taglineAlpha= remember { Animatable(0f) }
    val brandAlpha  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo pop in
        launch {
            logoAlpha.animateTo(1f, tween(600, easing = EaseOut))
        }
        launch {
            logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
        delay(500)
        // App name
        textAlpha.animateTo(1f, tween(500))
        delay(300)
        // Tagline
        taglineAlpha.animateTo(1f, tween(500))
        delay(300)
        // Brand footer
        brandAlpha.animateTo(1f, tween(600))
        delay(1200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark)
                    Brush.radialGradient(
                        colors = listOf(Navy800, Navy950),
                        radius = 1400f
                    )
                else
                    Brush.radialGradient(
                        colors = listOf(LightSurface2, LightBg),
                        radius = 1400f
                    )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glow ring in dark mode
        if (isDark) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .alpha(logoAlpha.value * 0.3f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Blue500.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // TaskPulse Logo
            Image(
                painter = painterResource(R.drawable.ic_taskpulse_logo),
                contentDescription = "TaskPulse",
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(Modifier.height(24.dp))

            // App Name
            Text(
                text = "TaskPulse",
                style = MaterialTheme.typography.displayLarge,
                color = if (isDark) TextOnDark else TextOnLight,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your phone. Your rules.",
                style = MaterialTheme.typography.titleMedium,
                color = Blue400,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(Modifier.height(60.dp))

            // Powered by chAs
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(brandAlpha.value)
            ) {
                Text(
                    text = "by",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) TextOnDark50 else TextOnLight50
                )
                Spacer(Modifier.height(8.dp))
                // chAs brand logo
                Image(
                    painter = painterResource(R.drawable.ic_chas_logo),
                    contentDescription = "chAs Technologies LLC",
                    modifier = Modifier.height(36.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "chAs Technologies LLC",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) TextOnDark50 else TextOnLight50,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
