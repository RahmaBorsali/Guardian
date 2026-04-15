package com.guardian.track.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.guardian.track.ui.screen.DashboardScreen
import com.guardian.track.ui.screen.HistoryScreen
import com.guardian.track.ui.screen.SettingsScreen

/**
 * Navigation graph hosting all three main screens.
 */
@Composable
fun GuardianNavigation(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onDarkModeChange: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(snackbarHostState = snackbarHostState)
        }
        composable(route = Screen.History.route) {
            HistoryScreen(snackbarHostState = snackbarHostState)
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                snackbarHostState = snackbarHostState,
                onDarkModeChange = onDarkModeChange
            )
        }
    }
}
