package com.chastechgroup.taskpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.chastechgroup.taskpulse.navigation.TaskPulseNavHost
import com.chastechgroup.taskpulse.services.MonitoringForegroundService
import com.chastechgroup.taskpulse.ui.theme.TaskPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Native splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start background monitoring service
        MonitoringForegroundService.start(this)

        setContent {
            TaskPulseTheme {
                val navController = rememberNavController()
                TaskPulseNavHost(navController = navController)
            }
        }
    }
}
