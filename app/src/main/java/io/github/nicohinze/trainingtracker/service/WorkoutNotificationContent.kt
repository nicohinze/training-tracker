package io.github.nicohinze.trainingtracker.service

import io.github.nicohinze.trainingtracker.viewmodel.ActiveState
import io.github.nicohinze.trainingtracker.viewmodel.ActiveWorkoutUiState

private const val MS_PER_SECOND = 1000L

/** A system-rendered timer in the notification, so it doesn't have to be re-posted every second. */
data class NotificationChronometer(
    val whenMs: Long,
    val countDown: Boolean,
)

data class WorkoutNotificationContent(
    val title: String,
    val text: String,
    val chronometer: NotificationChronometer?,
)

/** Builds the notification for an in-progress workout, with [nowMs] as wall-clock time. */
fun workoutNotificationContent(state: ActiveWorkoutUiState, nowMs: Long): WorkoutNotificationContent {
    val title = state.workout?.name ?: "Workout"
    val exercise = state.currentExercise
    return when (state.state) {
        ActiveState.RESTING -> {
            val next = state.restContext?.let { state.exercises.getOrNull(it.nextExerciseIndex) }
            WorkoutNotificationContent(
                title = title,
                text = if (next != null) "Rest — next: ${next.name}" else "Rest",
                chronometer = NotificationChronometer(
                    whenMs = nowMs + state.remainingSeconds * MS_PER_SECOND,
                    countDown = true,
                ),
            )
        }

        ActiveState.PAUSED -> {
            WorkoutNotificationContent(title = title, text = "Paused", chronometer = null)
        }

        else -> {
            WorkoutNotificationContent(
                title = title,
                text = if (exercise != null) {
                    "${exercise.name} — Set ${state.completedSets}/${exercise.sets}"
                } else {
                    ""
                },
                chronometer = NotificationChronometer(
                    whenMs = nowMs - state.elapsedSeconds * MS_PER_SECOND,
                    countDown = false,
                ),
            )
        }
    }
}
