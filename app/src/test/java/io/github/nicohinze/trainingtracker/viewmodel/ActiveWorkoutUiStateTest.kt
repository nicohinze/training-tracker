package io.github.nicohinze.trainingtracker.viewmodel

import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWorkoutUiStateTest {
    private val exercises = listOf(
        Exercise(id = 1, workoutId = 1, name = "Bench Press", sets = 3, amount = 10, pauseSeconds = 90, orderIndex = 0),
        Exercise(id = 2, workoutId = 1, name = "Squats", sets = 4, amount = 8, pauseSeconds = 120, orderIndex = 1),
        Exercise(id = 3, workoutId = 1, name = "Pull-ups", sets = 2, amount = 12, pauseSeconds = 60, orderIndex = 2),
    )

    @Test
    fun currentExercise_returnsExerciseAtIndex() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 1)
        assertEquals(exercises[1], state.currentExercise)
    }

    @Test
    fun currentExercise_returnsNullForEmptyExercises() {
        val state = ActiveWorkoutUiState(exercises = emptyList(), currentExerciseIndex = 0)
        assertNull(state.currentExercise)
    }

    @Test
    fun currentExercise_returnsNullForOutOfBoundsIndex() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 5)
        assertNull(state.currentExercise)
    }

    @Test
    fun totalSets_returnsSetsOfCurrentExercise() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 0)
        assertEquals(3, state.totalSets)

        val state2 = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 1)
        assertEquals(4, state2.totalSets)
    }

    @Test
    fun totalSets_returnsZeroWhenNoCurrentExercise() {
        val state = ActiveWorkoutUiState(exercises = emptyList())
        assertEquals(0, state.totalSets)
    }

    @Test
    fun isLastSetOfExercise_trueWhenCompletedSetsEqualsTotalSets() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 0, completedSets = 3)
        assertTrue(state.isLastSetOfExercise)
    }

    @Test
    fun isLastSetOfExercise_trueWhenCompletedSetsExceedsTotalSets() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 0, completedSets = 5)
        assertTrue(state.isLastSetOfExercise)
    }

    @Test
    fun isLastSetOfExercise_falseWhenSetsRemaining() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 0, completedSets = 2)
        assertFalse(state.isLastSetOfExercise)
    }

    @Test
    fun isLastExercise_trueForLastIndex() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 2)
        assertTrue(state.isLastExercise)
    }

    @Test
    fun isLastExercise_falseForMiddleIndex() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 1)
        assertFalse(state.isLastExercise)
    }

    @Test
    fun isLastExercise_falseForFirstIndex() {
        val state = ActiveWorkoutUiState(exercises = exercises, currentExerciseIndex = 0)
        assertFalse(state.isLastExercise)
    }

    @Test
    fun isLastExercise_trueForSingleExercise() {
        val single = listOf(exercises[0])
        val state = ActiveWorkoutUiState(exercises = single, currentExerciseIndex = 0)
        assertTrue(state.isLastExercise)
    }

    @Test
    fun isLastExercise_trueForEmptyExercises() {
        val state = ActiveWorkoutUiState(exercises = emptyList(), currentExerciseIndex = 0)
        assertTrue(state.isLastExercise)
    }

    @Test
    fun defaultState_isReady() {
        val state = ActiveWorkoutUiState()
        assertEquals(ActiveState.READY, state.state)
        assertNull(state.workout)
        assertTrue(state.exercises.isEmpty())
        assertEquals(0, state.currentExerciseIndex)
        assertEquals(0, state.completedSets)
        assertEquals(0, state.remainingSeconds)
    }

    @Test
    fun finishedState_atLastSetOfLastExercise() {
        val state = ActiveWorkoutUiState(
            workout = Workout(id = 1, name = "Test"),
            exercises = exercises,
            currentExerciseIndex = 2,
            completedSets = 2,
            state = ActiveState.FINISHED,
        )
        assertTrue(state.isLastExercise)
        assertTrue(state.isLastSetOfExercise)
        assertEquals(ActiveState.FINISHED, state.state)
    }
}
