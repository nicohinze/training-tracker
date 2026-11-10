package io.github.nicohinze.trainingtracker.viewmodel

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.nicohinze.trainingtracker.ActiveWorkoutRoute
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.milliseconds

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
private const val MS_PER_SECOND = 1000L

private const val KEY_STATE = "session_state"
private const val KEY_EXERCISE_INDEX = "session_exercise_index"
private const val KEY_COMPLETED_SETS = "session_completed_sets"
private const val KEY_ELAPSED_SECONDS = "session_elapsed_seconds"
private const val KEY_REST_NEXT_EXERCISE = "session_rest_next_exercise"
private const val KEY_REST_NEXT_SET = "session_rest_next_set"
private const val KEY_START_REALTIME = "session_start_realtime"
private const val KEY_PAUSED_AT_REALTIME = "session_paused_at_realtime"
private const val KEY_TOTAL_PAUSED_MS = "session_total_paused_ms"
private const val KEY_REST_END_REALTIME = "session_rest_end_realtime"
private const val KEY_STATE_BEFORE_PAUSE = "session_state_before_pause"
private const val KEY_REST_REMAINING_ON_PAUSE = "session_rest_remaining_on_pause"

/**
 * The whole session lives in [SavedStateHandle], so an active workout survives process death.
 * All timestamps come from [clock] (elapsedRealtime by default), which keeps counting while the process is dead.
 */
class ActiveWorkoutViewModel(
    private val dao: WorkoutDao,
    private val savedStateHandle: SavedStateHandle,
    private val onVibrate: () -> Unit = {},
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val workoutId: Long = checkNotNull(savedStateHandle.get<Long>(ActiveWorkoutRoute::workoutId.name)) {
        "workoutId nav argument is required for ActiveWorkoutViewModel"
    }
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState = _uiState.asStateFlow()

    // All state changes go through here so they are mirrored into savedStateHandle.
    private var ui: ActiveWorkoutUiState
        get() = _uiState.value
        set(value) {
            _uiState.value = value
            saveSession(value)
        }

    private var timerJob: Job? = null
    private var elapsedJob: Job? = null
    private var workoutStartRealtime by saved(KEY_START_REALTIME, 0L)
    private var pausedAtRealtime by saved(KEY_PAUSED_AT_REALTIME, 0L)
    private var totalPausedMs by saved(KEY_TOTAL_PAUSED_MS, 0L)
    private var restEndRealtime by saved(KEY_REST_END_REALTIME, 0L)
    private var stateBeforePause by saved(KEY_STATE_BEFORE_PAUSE, ActiveState.EXERCISING)
    private var restRemainingOnPause by saved(KEY_REST_REMAINING_ON_PAUSE, 0)

    init {
        viewModelScope.launch {
            val details = dao.getWorkoutWithExercises(workoutId)
            val loaded = ActiveWorkoutUiState(
                workout = details?.workout,
                exercises = details?.exercises?.sortedBy { it.orderIndex } ?: emptyList(),
                state = ActiveState.READY,
                workoutNotFound = details?.workout == null,
            )
            ui = restoreSession(loaded) ?: loaded
            if (ui.state == ActiveState.EXERCISING || ui.state == ActiveState.RESTING) {
                startElapsedTimer()
            }
            if (ui.state == ActiveState.RESTING) {
                startRestTimer()
            }
        }
    }

    fun startWorkout() {
        if (ui.exercises.isEmpty()) {
            return
        }
        workoutStartRealtime = clock()
        totalPausedMs = 0L
        ui = ui.copy(
            state = ActiveState.EXERCISING,
            currentExerciseIndex = 0,
            completedSets = 0,
            elapsedSeconds = 0,
        )
        startElapsedTimer()
    }

    fun pauseWorkout() {
        val current = ui
        if (current.state != ActiveState.EXERCISING && current.state != ActiveState.RESTING) {
            return
        }
        stateBeforePause = current.state
        pausedAtRealtime = clock()
        if (current.state == ActiveState.RESTING) {
            restRemainingOnPause = current.remainingSeconds
            timerJob?.cancel()
        }
        elapsedJob?.cancel()
        ui = current.copy(state = ActiveState.PAUSED)
    }

    fun resumeWorkout() {
        if (ui.state != ActiveState.PAUSED) {
            return
        }
        val pauseDuration = clock() - pausedAtRealtime
        totalPausedMs += pauseDuration
        startElapsedTimer()
        if (stateBeforePause == ActiveState.RESTING) {
            restEndRealtime = clock() + restRemainingOnPause * MS_PER_SECOND
            ui = ui.copy(
                state = ActiveState.RESTING,
                remainingSeconds = restRemainingOnPause,
            )
            startRestTimer()
        } else {
            ui = ui.copy(state = ActiveState.EXERCISING)
        }
    }

    private fun startElapsedTimer() {
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (true) {
                delay(MS_PER_SECOND.milliseconds)
                ui = ui.copy(elapsedSeconds = currentElapsedSeconds())
            }
        }
    }

    private fun currentElapsedSeconds(): Long = (clock() - workoutStartRealtime - totalPausedMs) / MS_PER_SECOND

    fun onSetFinished() {
        val current = ui
        if (current.state != ActiveState.EXERCISING) {
            return
        }
        val newCompleted = current.completedSets + 1
        val updated = current.copy(completedSets = newCompleted)
        if (updated.isLastSetOfExercise && updated.isLastExercise) {
            elapsedJob?.cancel()
            val finalElapsed = currentElapsedSeconds()
            ui = updated.copy(state = ActiveState.FINISHED, elapsedSeconds = finalElapsed)
            viewModelScope.launch {
                // Leaving the screen right away clears the ViewModel, which must not cancel this write
                withContext(NonCancellable) {
                    dao.completeWorkout(workoutId, finalElapsed)
                }
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
            ui = ui.copy(
                state = ActiveState.EXERCISING,
                currentExerciseIndex = nextExerciseIndex,
                completedSets = nextSet,
            )
            return
        }
        restEndRealtime = clock() + pauseSeconds * MS_PER_SECOND
        ui = ui.copy(
            state = ActiveState.RESTING,
            currentExerciseIndex = restExerciseIndex,
            completedSets = restSet,
            remainingSeconds = pauseSeconds,
            restContext = RestContext(nextExerciseIndex = nextExerciseIndex, nextSet = nextSet),
        )
        startRestTimer()
    }

    private fun remainingRestSeconds(): Int {
        val remainingMs = (restEndRealtime - clock()).coerceAtLeast(0)
        return ((remainingMs + MS_PER_SECOND - 1) / MS_PER_SECOND).toInt()
    }

    private fun startRestTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val remainingMs = restEndRealtime - clock()
                if (remainingMs <= 0) {
                    finishRest()
                    break
                }
                // Wake up exactly when the displayed seconds value changes.
                val msUntilNextTick = (remainingMs % MS_PER_SECOND).takeIf { it > 0 } ?: MS_PER_SECOND
                delay(msUntilNextTick.milliseconds)
                val remaining = remainingRestSeconds()
                if (remaining > 0) {
                    if (remaining <= VIBRATION_COUNTDOWN_THRESHOLD) {
                        onVibrate()
                    }
                    ui = ui.copy(remainingSeconds = remaining)
                }
            }
        }
    }

    private fun finishRest() {
        val ctx = ui.restContext!!
        ui = ui.copy(
            state = ActiveState.EXERCISING,
            currentExerciseIndex = ctx.nextExerciseIndex,
            completedSets = ctx.nextSet,
            remainingSeconds = 0,
            restContext = null,
        )
    }

    private fun saveSession(state: ActiveWorkoutUiState) {
        savedStateHandle[KEY_STATE] = state.state
        savedStateHandle[KEY_EXERCISE_INDEX] = state.currentExerciseIndex
        savedStateHandle[KEY_COMPLETED_SETS] = state.completedSets
        savedStateHandle[KEY_ELAPSED_SECONDS] = state.elapsedSeconds
        savedStateHandle[KEY_REST_NEXT_EXERCISE] = state.restContext?.nextExerciseIndex
        savedStateHandle[KEY_REST_NEXT_SET] = state.restContext?.nextSet
    }

    /** Returns the saved session applied to [loaded], or null if there is none or it doesn't fit the workout. */
    private fun restoreSession(loaded: ActiveWorkoutUiState): ActiveWorkoutUiState? {
        val state = savedStateHandle.get<ActiveState>(KEY_STATE)
        val index = savedStateHandle.get<Int>(KEY_EXERCISE_INDEX)
        val restContext = savedRestContext()
        val inRest = state == ActiveState.RESTING ||
            (state == ActiveState.PAUSED && stateBeforePause == ActiveState.RESTING)
        val isValid = state != null &&
            state != ActiveState.READY &&
            index != null &&
            index in loaded.exercises.indices &&
            (!inRest || restContext != null)
        if (!isValid) {
            return null
        }
        return loaded.copy(
            state = state,
            currentExerciseIndex = index,
            completedSets = savedStateHandle.get<Int>(KEY_COMPLETED_SETS) ?: 0,
            elapsedSeconds = savedStateHandle.get<Long>(KEY_ELAPSED_SECONDS) ?: 0L,
            remainingSeconds = when {
                state == ActiveState.RESTING -> remainingRestSeconds()
                inRest -> restRemainingOnPause
                else -> 0
            },
            restContext = restContext,
        )
    }

    private fun savedRestContext(): RestContext? {
        val nextExercise = savedStateHandle.get<Int>(KEY_REST_NEXT_EXERCISE)
        val nextSet = savedStateHandle.get<Int>(KEY_REST_NEXT_SET)
        return if (nextExercise != null && nextSet != null) RestContext(nextExercise, nextSet) else null
    }

    private fun <T : Any> saved(key: String, default: T) = object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = savedStateHandle[key] ?: default

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            savedStateHandle[key] = value
        }
    }

    companion object {
        fun factory(dao: WorkoutDao, onVibrate: () -> Unit) = viewModelFactory {
            initializer { ActiveWorkoutViewModel(dao, createSavedStateHandle(), onVibrate) }
        }
    }
}
