package io.github.nicohinze.trainingtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.nicohinze.trainingtracker.WorkoutApplication
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.Workout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ActiveState {
    READY,
    EXERCISING,
    RESTING,
    FINISHED,
}

data class ActiveWorkoutUiState(
    val workout: Workout? = null,
    val exercises: List<Exercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val completedSets: Int = 0,
    val state: ActiveState = ActiveState.READY,
    val remainingSeconds: Int = 0,
) {
    val currentExercise: Exercise? get() = exercises.getOrNull(currentExerciseIndex)
    val totalSets: Int get() = currentExercise?.sets ?: 0
    val isLastSetOfExercise: Boolean get() = completedSets >= totalSets
    val isLastExercise: Boolean get() = currentExerciseIndex >= exercises.size - 1
}

class ActiveWorkoutViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val dao = (application as WorkoutApplication).database.workoutDao()
    private val workoutId: Long = savedStateHandle.get<Long>("workoutId") ?: 0L
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState = _uiState.asStateFlow()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val (workout, exercises) = dao.getWorkoutWithExercises(workoutId)
            _uiState.value = ActiveWorkoutUiState(
                workout = workout,
                exercises = exercises,
                state = ActiveState.READY,
            )
        }
    }

    fun startWorkout() {
        if (_uiState.value.exercises.isEmpty()) {
            return
        }
        _uiState.value = _uiState.value.copy(
            state = ActiveState.EXERCISING,
            currentExerciseIndex = 0,
            completedSets = 0,
        )
    }

    fun onSetFinished() {
        val current = _uiState.value
        if (current.state != ActiveState.EXERCISING) {
            return
        }
        val newCompleted = current.completedSets + 1
        val updated = current.copy(completedSets = newCompleted)
        if (updated.isLastSetOfExercise && updated.isLastExercise) {
            _uiState.value = updated.copy(state = ActiveState.FINISHED)
            viewModelScope.launch { dao.incrementCompletionCount(workoutId) }
            return
        }
        if (updated.isLastSetOfExercise) {
            val nextIndex = updated.currentExerciseIndex + 1
            val currentExercise = updated.currentExercise!!
            startRest(
                pauseSeconds = currentExercise.pauseSeconds,
                restExerciseIndex = updated.currentExerciseIndex,
                restSet = newCompleted,
                nextExerciseIndex = nextIndex,
                nextSet = 0,
            )
        } else {
            val exercise = updated.currentExercise!!
            startRest(
                pauseSeconds = exercise.pauseSeconds,
                restExerciseIndex = updated.currentExerciseIndex,
                restSet = newCompleted,
                nextExerciseIndex = updated.currentExerciseIndex,
                nextSet = newCompleted,
            )
        }
    }

    private fun startRest(
        pauseSeconds: Int,
        restExerciseIndex: Int,
        restSet: Int,
        nextExerciseIndex: Int,
        nextSet: Int,
    ) {
        if (pauseSeconds <= 0) {
            _uiState.value = _uiState.value.copy(
                state = ActiveState.EXERCISING,
                currentExerciseIndex = nextExerciseIndex,
                completedSets = nextSet,
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            state = ActiveState.RESTING,
            currentExerciseIndex = restExerciseIndex,
            completedSets = restSet,
            remainingSeconds = pauseSeconds,
        )
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (remaining in pauseSeconds - 1 downTo 0) {
                delay(1000L)
                if (remaining > 0) {
                    _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
                } else {
                    _uiState.value = _uiState.value.copy(
                        state = ActiveState.EXERCISING,
                        currentExerciseIndex = nextExerciseIndex,
                        completedSets = nextSet,
                        remainingSeconds = 0,
                    )
                }
            }
        }
    }
}
