package io.github.nicohinze.trainingtracker.viewmodel

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ActiveState {
    READY,
    EXERCISING,
    RESTING,
    PAUSED,
    FINISHED,
}

data class RestContext(
    val nextExerciseIndex: Int,
    val nextSet: Int,
)

data class ActiveWorkoutUiState(
    val workout: Workout? = null,
    val exercises: List<Exercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val completedSets: Int = 0,
    val state: ActiveState = ActiveState.READY,
    val remainingSeconds: Int = 0,
    val elapsedSeconds: Long = 0,
    val workoutNotFound: Boolean = false,
    val restContext: RestContext? = null,
) {
    val currentExercise: Exercise? get() = exercises.getOrNull(currentExerciseIndex)
    val totalSets: Int get() = currentExercise?.sets ?: 0
    val isLastSetOfExercise: Boolean get() = completedSets >= totalSets
    val isLastExercise: Boolean get() = currentExerciseIndex >= exercises.size - 1
}

private const val VIBRATION_COUNTDOWN_THRESHOLD = 3

class ActiveWorkoutViewModel(
    private val dao: WorkoutDao,
    savedStateHandle: SavedStateHandle,
    private val onVibrate: () -> Unit = {},
) : ViewModel() {
    private val workoutId: Long = checkNotNull(savedStateHandle.get<Long>("workoutId")) {
        "workoutId nav argument is required for ActiveWorkoutViewModel"
    }
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState = _uiState.asStateFlow()
    private var timerJob: Job? = null
    private var elapsedJob: Job? = null
    private var workoutStartRealtime: Long = 0L
    private var pausedAtRealtime: Long = 0L
    private var totalPausedMs: Long = 0L
    private var stateBeforePause: ActiveState = ActiveState.EXERCISING
    private var restRemainingOnPause: Int = 0

    init {
        viewModelScope.launch {
            val (workout, exercises) = dao.getWorkoutWithExercises(workoutId)
            _uiState.value = ActiveWorkoutUiState(
                workout = workout,
                exercises = exercises,
                state = ActiveState.READY,
                workoutNotFound = workout == null,
            )
        }
    }

    fun startWorkout() {
        if (_uiState.value.exercises.isEmpty()) {
            return
        }
        workoutStartRealtime = SystemClock.elapsedRealtime()
        totalPausedMs = 0L
        _uiState.value = _uiState.value.copy(
            state = ActiveState.EXERCISING,
            currentExerciseIndex = 0,
            completedSets = 0,
            elapsedSeconds = 0,
        )
        startElapsedTimer()
    }

    fun pauseWorkout() {
        val current = _uiState.value
        if (current.state != ActiveState.EXERCISING && current.state != ActiveState.RESTING) {
            return
        }
        stateBeforePause = current.state
        pausedAtRealtime = SystemClock.elapsedRealtime()
        if (current.state == ActiveState.RESTING) {
            restRemainingOnPause = current.remainingSeconds
            timerJob?.cancel()
        }
        elapsedJob?.cancel()
        _uiState.value = current.copy(state = ActiveState.PAUSED)
    }

    fun resumeWorkout() {
        if (_uiState.value.state != ActiveState.PAUSED) {
            return
        }
        val pauseDuration = SystemClock.elapsedRealtime() - pausedAtRealtime
        totalPausedMs += pauseDuration
        startElapsedTimer()
        if (stateBeforePause == ActiveState.RESTING) {
            _uiState.value = _uiState.value.copy(
                state = ActiveState.RESTING,
                remainingSeconds = restRemainingOnPause,
            )
            startRestTimer(restRemainingOnPause)
        } else {
            _uiState.value = _uiState.value.copy(state = ActiveState.EXERCISING)
        }
    }

    private fun startElapsedTimer() {
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val elapsed = (SystemClock.elapsedRealtime() - workoutStartRealtime - totalPausedMs) / 1000
                _uiState.value = _uiState.value.copy(elapsedSeconds = elapsed)
            }
        }
    }

    fun onSetFinished() {
        val current = _uiState.value
        if (current.state != ActiveState.EXERCISING) {
            return
        }
        val newCompleted = current.completedSets + 1
        val updated = current.copy(completedSets = newCompleted)
        if (updated.isLastSetOfExercise && updated.isLastExercise) {
            elapsedJob?.cancel()
            val finalElapsed = (SystemClock.elapsedRealtime() - workoutStartRealtime - totalPausedMs) / 1000
            _uiState.value = updated.copy(state = ActiveState.FINISHED, elapsedSeconds = finalElapsed)
            viewModelScope.launch {
                dao.completeWorkout(workoutId, finalElapsed)
            }
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
            restContext = RestContext(nextExerciseIndex = nextExerciseIndex, nextSet = nextSet),
        )
        startRestTimer(pauseSeconds)
    }

    private fun startRestTimer(totalSeconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val restStartRealtime = SystemClock.elapsedRealtime()
            while (true) {
                delay(1000L)
                val elapsedMs = SystemClock.elapsedRealtime() - restStartRealtime
                val remaining = totalSeconds - (elapsedMs / 1000).toInt()
                if (remaining > 0) {
                    if (remaining in 1..VIBRATION_COUNTDOWN_THRESHOLD) {
                        onVibrate()
                    }
                    _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
                } else {
                    val ctx = _uiState.value.restContext
                        ?: break
                    _uiState.value = _uiState.value.copy(
                        state = ActiveState.EXERCISING,
                        currentExerciseIndex = ctx.nextExerciseIndex,
                        completedSets = ctx.nextSet,
                        remainingSeconds = 0,
                        restContext = null,
                    )
                    break
                }
            }
        }
    }

    companion object {
        fun factory(dao: WorkoutDao, onVibrate: () -> Unit) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T = ActiveWorkoutViewModel(dao, extras.createSavedStateHandle(), onVibrate) as T
        }
    }
}
