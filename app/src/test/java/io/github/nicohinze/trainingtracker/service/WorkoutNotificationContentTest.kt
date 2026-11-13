package io.github.nicohinze.trainingtracker.service

import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.viewmodel.ActiveState
import io.github.nicohinze.trainingtracker.viewmodel.ActiveWorkoutUiState
import io.github.nicohinze.trainingtracker.viewmodel.RestContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutNotificationContentTest {
    private val now = 1_000_000L
    private val state = ActiveWorkoutUiState(
        workout = Workout(id = 1, name = "Push Day"),
        exercises = listOf(
            Exercise(id = 1, workoutId = 1, name = "Bench Press", sets = 3, amount = 10, pauseSeconds = 90),
            Exercise(id = 2, workoutId = 1, name = "Dips", sets = 2, amount = 8, pauseSeconds = 60),
        ),
    )

    @Test
    fun exercising_showsSetProgressAndCountsUpElapsedTime() {
        val content = workoutNotificationContent(
            state.copy(state = ActiveState.EXERCISING, completedSets = 1, elapsedSeconds = 30),
            now,
        )
        assertEquals("Push Day", content.title)
        assertEquals("Bench Press — Set 1/3", content.text)
        assertEquals(NotificationChronometer(whenMs = now - 30_000, countDown = false), content.chronometer)
    }

    @Test
    fun resting_showsNextExerciseAndCountsDownToRestEnd() {
        val content = workoutNotificationContent(
            state.copy(
                state = ActiveState.RESTING,
                completedSets = 3,
                remainingSeconds = 45,
                restContext = RestContext(nextExerciseIndex = 1, nextSet = 0),
            ),
            now,
        )
        assertEquals("Rest — next: Dips", content.text)
        assertEquals(NotificationChronometer(whenMs = now + 45_000, countDown = true), content.chronometer)
    }

    @Test
    fun paused_hasNoChronometer() {
        val content = workoutNotificationContent(state.copy(state = ActiveState.PAUSED), now)
        assertEquals("Paused", content.text)
        assertNull(content.chronometer)
    }
}
