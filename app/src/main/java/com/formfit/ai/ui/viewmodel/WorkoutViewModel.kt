package com.formfit.ai.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.formfit.ai.data.model.User
import com.formfit.ai.data.model.WorkoutResult

class WorkoutViewModel : ViewModel() {

    var currentUser by mutableStateOf<User?>(null)
        private set

    var selectedExerciseId by mutableIntStateOf(1)
        private set

    var lastWorkoutResult by mutableStateOf<WorkoutResult?>(null)
        private set

    fun updateUser(user: User) { currentUser = user }
    fun updateExerciseId(id: Int) { selectedExerciseId = id }
    fun updateWorkoutResult(result: WorkoutResult) { lastWorkoutResult = result }
}
