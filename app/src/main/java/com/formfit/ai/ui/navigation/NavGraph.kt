package com.formfit.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.formfit.ai.data.FakeDataSource
import com.formfit.ai.data.model.User
import com.formfit.ai.ui.screens.AnalysisScreen
import com.formfit.ai.ui.screens.HomeScreen
import com.formfit.ai.ui.screens.LoginScreen
import com.formfit.ai.ui.screens.SummaryScreen
import com.formfit.ai.ui.viewmodel.WorkoutViewModel

object Routes {
    const val LOGIN    = "login"
    const val HOME     = "home"
    const val ANALYSIS = "analysis"
    const val SUMMARY  = "summary"
}

@Composable
fun FitFormNavGraph(navController: NavHostController) {
    val viewModel = viewModel<WorkoutViewModel>()

    NavHost(
        navController    = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { name, email, emoji ->
                    viewModel.updateUser(
                        User(
                            kullaniciID  = 1,
                            kullaniciAdi = name.ifBlank { "Kullanıcı" },
                            email        = email,
                            profilEmoji  = emoji,
                            kilo         = null,
                            boy          = null,
                            cinsiyet     = null
                        )
                    )
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                user           = viewModel.currentUser,
                onStartWorkout = { exerciseId ->
                    viewModel.updateExerciseId(exerciseId)
                    navController.navigate(Routes.ANALYSIS)
                }
            )
        }

        composable(Routes.ANALYSIS) {
            AnalysisScreen(
                exerciseId  = viewModel.selectedExerciseId,
                kullaniciID = viewModel.currentUser?.kullaniciID ?: 1,
                onFinish    = { result ->
                    val saved = FakeDataSource.saveWorkoutResult(result)
                    viewModel.updateWorkoutResult(saved)
                    navController.navigate(Routes.SUMMARY) {
                        popUpTo(Routes.ANALYSIS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SUMMARY) {
            val result = viewModel.lastWorkoutResult
            if (result != null) {
                SummaryScreen(
                    workoutResult = result,
                    onBackToHome  = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
