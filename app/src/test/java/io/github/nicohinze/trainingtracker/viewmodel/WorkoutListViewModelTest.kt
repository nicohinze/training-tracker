package io.github.nicohinze.trainingtracker.viewmodel

import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutCompletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutListViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: FakeWorkoutDao
    private lateinit var vm: WorkoutListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeWorkoutDao()
        vm = WorkoutListViewModel(dao, ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addWorkout_insertsWorkoutIntoDao() = runTest(testDispatcher) {
        vm.addWorkout("Push Day", 0xFF2196F3.toInt())
        assertEquals(1, dao.workouts.size)
        assertEquals("Push Day", dao.workouts[0].name)
        assertEquals(0xFF2196F3.toInt(), dao.workouts[0].color)
    }

    @Test
    fun deleteWorkout_removesFromDao() = runTest(testDispatcher) {
        val workout = Workout(id = 1, name = "Leg Day")
        dao.seed(workout)
        vm.deleteWorkout(workout)
        assertTrue(dao.workouts.isEmpty())
    }

    @Test
    fun updateWorkout_savesNewNameAndColor() = runTest(testDispatcher) {
        val workout = Workout(id = 1, name = "Old Name", color = 0xFF2196F3.toInt())
        dao.seed(workout)
        vm.updateWorkout(workout, "New Name", 0xFF4CAF50.toInt())
        assertEquals("New Name", dao.workouts[0].name)
        assertEquals(0xFF4CAF50.toInt(), dao.workouts[0].color)
    }

    @Test
    fun resetAllStats_clearsCountersAndCompletions() = runTest(testDispatcher) {
        dao.seed(Workout(id = 1, name = "W", completionCount = 5, totalDurationSeconds = 3600))
        dao.insertCompletion(WorkoutCompletion(workoutId = 1, completedAt = 1000, durationSeconds = 60))
        vm.resetAllStats()
        assertEquals(0, dao.workouts[0].completionCount)
        assertEquals(0L, dao.workouts[0].totalDurationSeconds)
        assertTrue(dao.completions.isEmpty())
    }

    @Test
    fun importJson_returnsImportedWorkoutCount() = runTest(testDispatcher) {
        val json = """{"workouts":[{"name":"A","exercises":[]},{"name":"B","exercises":[]}]}"""
        val result = vm.importJson(json)
        assertEquals(2, result.getOrNull())
        assertEquals(2, dao.workouts.size)
    }

    @Test
    fun importJson_returnsFailureForInvalidJson() = runTest(testDispatcher) {
        val result = vm.importJson("not json")
        assertTrue(result.isFailure)
    }

    @Test
    fun exportJson_includesWorkoutsFromDao() = runTest(testDispatcher) {
        dao.seed(Workout(id = 1, name = "Push Day"))
        dao.seed(Workout(id = 2, name = "Pull Day"))
        val json = vm.exportJson()
        assertTrue(json.contains("Push Day"))
        assertTrue(json.contains("Pull Day"))
    }

    @Test
    fun importWorkouts_reportsImportedCount() = runTest(testDispatcher) {
        vm.importWorkouts { """{"workouts":[{"name":"A","exercises":[]}]}""" }
        assertEquals("Imported 1 workout(s)", vm.messages.first())
        assertEquals(1, dao.workouts.size)
    }

    @Test
    fun importWorkouts_reportsInvalidFile() = runTest(testDispatcher) {
        vm.importWorkouts { "not json" }
        assertEquals("Import failed: invalid file", vm.messages.first())
        assertTrue(dao.workouts.isEmpty())
    }

    @Test
    fun importWorkouts_reportsReadFailure() = runTest(testDispatcher) {
        vm.importWorkouts { throw IOException("boom") }
        assertEquals("Import failed: could not read file", vm.messages.first())
    }

    @Test
    fun exportWorkouts_writesJsonAndReportsSuccess() = runTest(testDispatcher) {
        dao.seed(Workout(id = 1, name = "Push Day"))
        var written = ""
        vm.exportWorkouts { written = it }
        assertEquals("Workouts exported", vm.messages.first())
        assertTrue(written.contains("Push Day"))
    }

    @Test
    fun exportWorkouts_reportsWriteFailure() = runTest(testDispatcher) {
        vm.exportWorkouts { throw IOException("boom") }
        assertEquals("Export failed: could not write file", vm.messages.first())
    }
}
