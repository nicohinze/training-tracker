package io.github.nicohinze.trainingtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutCompletion
import io.github.nicohinze.trainingtracker.data.WorkoutDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit

data class ActivityGraphUiState(
    val completions: List<WorkoutCompletion> = emptyList(),
    val workouts: List<Workout> = emptyList(),
)

class ActivityGraphViewModel(
    dao: WorkoutDao,
) : ViewModel() {
    val uiState = flow {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(HISTORY_DAYS)
        emitAll(
            combine(
                dao.getCompletionsSince(since),
                dao.getAllWorkouts(),
            ) { completions, workouts ->
                ActivityGraphUiState(completions = completions, workouts = workouts)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActivityGraphUiState())

    companion object {
        private const val HISTORY_DAYS = 365L

        fun factory(dao: WorkoutDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ActivityGraphViewModel(dao) as T
        }
    }
}
