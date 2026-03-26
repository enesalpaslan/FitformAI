package com.formfit.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.formfit.ai.ui.screens.AnalysisScreen
import com.formfit.ai.ui.screens.HomeScreen
import com.formfit.ai.ui.screens.LoginScreen
import com.formfit.ai.ui.screens.SummaryScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ANALYSIS = "analysis"
    const val SUMMARY = "summary"
}

@Composable
fun FitFormNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onStartWorkout = {
                    navController.navigate(Routes.ANALYSIS)
                }
            )
        }

        composable(Routes.ANALYSIS) {
            AnalysisScreen(
                onFinish = {
                    navController.navigate(Routes.SUMMARY) {
                        popUpTo(Routes.ANALYSIS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SUMMARY) {
            SummaryScreen(
                onBackToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
