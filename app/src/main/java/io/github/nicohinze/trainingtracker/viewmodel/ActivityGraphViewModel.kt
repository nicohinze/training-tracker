package io.github.nicohinze.trainingtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nicohinze.trainingtracker.WorkoutApplication
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutCompletion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ActivityGraphUiState(
    val completions: List<WorkoutCompletion> = emptyList(),
    val workouts: List<Workout> = emptyList(),
)

class ActivityGraphViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val dao = (application as WorkoutApplication).database.workoutDao()
    private val since = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000

    val uiState = combine(
        dao.getCompletionsSince(since),
        dao.getAllWorkouts(),
    ) { completions, workouts ->
        ActivityGraphUiState(completions = completions, workouts = workouts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActivityGraphUiState())
}
