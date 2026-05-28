package com.chastechgroup.taskpulse.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chastechgroup.taskpulse.ui.screens.*

sealed class Screen(val route: String) {
    object Splash   : Screen("splash")
    object Home     : Screen("home")
    object Rules    : Screen("rules")
    object Apps     : Screen("apps")
    object Store    : Screen("store")
    object Permissions : Screen("permissions")
}

@Composable
fun TaskPulseNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 }
        },
        exitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 }
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onFinished = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToRules = { navController.navigate(Screen.Rules.route) },
                onNavigateToApps = { navController.navigate(Screen.Apps.route) },
                onNavigateToStore = { navController.navigate(Screen.Store.route) },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
            )
        }
        composable(Screen.Rules.route) {
            RulesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Apps.route) {
            AppsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Store.route) {
            StoreScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(onBack = { navController.popBackStack() })
        }
    }
}
