package io.github.nicohinze.trainingtracker.viewmodel

import androidx.lifecycle.SavedStateHandle
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.ExerciseType
import io.github.nicohinze.trainingtracker.data.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutEditViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: FakeWorkoutDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeWorkoutDao()
        dao.seed(Workout(id = 1, name = "Push Day"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(workoutId: Long = 1L) =
        WorkoutEditViewModel(dao, SavedStateHandle(mapOf("workoutId" to workoutId)))

    private fun exercise(id: Long, name: String, orderIndex: Int) =
        Exercise(id = id, workoutId = 1, name = name, sets = 3, amount = 10, pauseSeconds = 60, orderIndex = orderIndex)

    @Test
    fun init_loadsWorkoutFromDao() = runTest(testDispatcher) {
        val vm = createVm()
        assertNotNull(vm.workout.value)
        assertEquals("Push Day", vm.workout.value?.name)
    }

    @Test
    fun addExercise_insertsIntoDao() = runTest(testDispatcher) {
        val vm = createVm()
        vm.addExercise(
            "Bench Press",
            sets = 3,
            amount = 10,
            type = ExerciseType.REPS,
            intensity = null,
            pauseSeconds = 90,
        )
        assertEquals(1, dao.exercises.size)
        assertEquals("Bench Press", dao.exercises[0].name)
        assertEquals(3, dao.exercises[0].sets)
        assertEquals(10, dao.exercises[0].amount)
        assertEquals(90, dao.exercises[0].pauseSeconds)
    }

    @Test
    fun addExercise_assignsNextOrderIndex() = runTest(testDispatcher) {
        dao.seed(exercise(id = 1, name = "First", orderIndex = 0))
        val vm = createVm()
        vm.addExercise("Second", sets = 2, amount = 8, type = ExerciseType.REPS, intensity = null, pauseSeconds = 60)
        val added = dao.exercises.first { it.name == "Second" }
        assertEquals(1, added.orderIndex)
    }

    @Test
    fun updateExercise_updatesFieldsInDao() = runTest(testDispatcher) {
        val exercise = Exercise(id = 1, workoutId = 1, name = "Bench Press", sets = 3, amount = 10, pauseSeconds = 90)
        dao.seed(exercise)
        val vm = createVm()
        vm.updateExercise(
            exercise,
            "Overhead Press",
            sets = 4,
            amount = 8,
            type = ExerciseType.REPS,
            intensity = "60kg",
            pauseSeconds = 60,
        )
        val updated = dao.exercises.first { it.id == 1L }
        assertEquals("Overhead Press", updated.name)
        assertEquals(4, updated.sets)
        assertEquals(8, updated.amount)
        assertEquals("60kg", updated.intensity)
        assertEquals(60, updated.pauseSeconds)
    }

    @Test
    fun deleteExercise_removesFromDao() = runTest(testDispatcher) {
        val exercise = Exercise(id = 1, workoutId = 1, name = "Bench Press", sets = 3, amount = 10, pauseSeconds = 90)
        dao.seed(exercise)
        val vm = createVm()
        vm.deleteExercise(exercise)
        assertTrue(dao.exercises.isEmpty())
    }

    @Test
    fun moveExercise_reordersExercisesInDao() = runTest(testDispatcher) {
        dao.seed(exercise(id = 1, name = "First", orderIndex = 0))
        dao.seed(exercise(id = 2, name = "Second", orderIndex = 1))
        dao.seed(exercise(id = 3, name = "Third", orderIndex = 2))
        val vm = createVm()
        val collectJob = launch { vm.exercises.collect {} }
        vm.moveExercise(from = 0, to = 2)
        advanceUntilIdle()
        val reordered = dao.exercises.sortedBy { it.orderIndex }
        assertEquals("Second", reordered[0].name)
        assertEquals("Third", reordered[1].name)
        assertEquals("First", reordered[2].name)
        collectJob.cancel()
    }
}
