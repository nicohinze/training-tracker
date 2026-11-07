package io.github.nicohinze.trainingtracker.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.Workout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: FakeWorkoutDao
    private val createdVms = mutableListOf<ActiveWorkoutViewModel>()
    private var vibrations = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeWorkoutDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(
        workoutId: Long = 1L,
        handle: SavedStateHandle = SavedStateHandle(mapOf("workoutId" to workoutId)),
    ) = ActiveWorkoutViewModel(
        dao,
        handle,
        onVibrate = { vibrations++ },
        clock = { testDispatcher.scheduler.currentTime },
    ).also { createdVms += it }

    // Simulates process death: the old ViewModel's coroutines die, a new one is built from the same saved state.
    private fun recreate(old: ActiveWorkoutViewModel, handle: SavedStateHandle): ActiveWorkoutViewModel {
        old.viewModelScope.cancel()
        return createVm(handle = handle)
    }

    // The ViewModel's timers loop forever on the test scheduler, so its scope must be
    // cancelled before runTest finishes, otherwise runTest never completes.
    private fun runVmTest(body: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            body()
        } finally {
            createdVms.forEach { it.viewModelScope.cancel() }
        }
    }

    private fun seedStandardWorkout(vararg exercises: Exercise) {
        dao.seed(Workout(id = 1, name = "Test Workout"))
        exercises.forEach { dao.seed(it) }
    }

    private fun twoExerciseWorkout() {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Ex A", sets = 3, amount = 10, pauseSeconds = 90, orderIndex = 0),
            Exercise(id = 2, workoutId = 1, name = "Ex B", sets = 2, amount = 8, pauseSeconds = 60, orderIndex = 1),
        )
    }

    @Test
    fun init_loadsWorkoutAndExercises() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        assertNotNull(vm.uiState.value.workout)
        assertEquals("Test Workout", vm.uiState.value.workout?.name)
        assertEquals(2, vm.uiState.value.exercises.size)
        assertEquals(ActiveState.READY, vm.uiState.value.state)
        assertFalse(vm.uiState.value.workoutNotFound)
    }

    @Test
    fun init_setsWorkoutNotFoundWhenMissing() = runVmTest {
        val vm = createVm(workoutId = 99L)
        assertTrue(vm.uiState.value.workoutNotFound)
        assertNull(vm.uiState.value.workout)
    }

    @Test
    fun startWorkout_transitionsToExercising() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.startWorkout()
        assertEquals(ActiveState.EXERCISING, vm.uiState.value.state)
        assertEquals(0, vm.uiState.value.currentExerciseIndex)
        assertEquals(0, vm.uiState.value.completedSets)
    }

    @Test
    fun startWorkout_doesNothingWhenNoExercises() = runVmTest {
        dao.seed(Workout(id = 1, name = "Empty"))
        val vm = createVm()
        vm.startWorkout()
        assertEquals(ActiveState.READY, vm.uiState.value.state)
    }

    @Test
    fun pauseWorkout_fromExercising_transitionsToPaused() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.startWorkout()
        vm.pauseWorkout()
        assertEquals(ActiveState.PAUSED, vm.uiState.value.state)
    }

    @Test
    fun pauseWorkout_fromReady_doesNothing() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.pauseWorkout()
        assertEquals(ActiveState.READY, vm.uiState.value.state)
    }

    @Test
    fun resumeWorkout_fromPaused_restoresExercising() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.startWorkout()
        vm.pauseWorkout()
        vm.resumeWorkout()
        assertEquals(ActiveState.EXERCISING, vm.uiState.value.state)
    }

    @Test
    fun resumeWorkout_fromPaused_restoresResting() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Ex A", sets = 1, amount = 10, pauseSeconds = 90, orderIndex = 0),
            Exercise(id = 2, workoutId = 1, name = "Ex B", sets = 1, amount = 8, pauseSeconds = 60, orderIndex = 1),
        )
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        assertEquals(ActiveState.RESTING, vm.uiState.value.state)
        vm.pauseWorkout()
        vm.resumeWorkout()
        assertEquals(ActiveState.RESTING, vm.uiState.value.state)
        assertEquals(90, vm.uiState.value.remainingSeconds)
    }

    @Test
    fun resumeWorkout_whenNotPaused_doesNothing() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.startWorkout()
        vm.resumeWorkout()
        assertEquals(ActiveState.EXERCISING, vm.uiState.value.state)
    }

    @Test
    fun onSetFinished_transitionsToRestingWithCorrectSeconds() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        assertEquals(ActiveState.RESTING, vm.uiState.value.state)
        assertEquals(90, vm.uiState.value.remainingSeconds)
        assertEquals(1, vm.uiState.value.completedSets)
    }

    @Test
    fun onSetFinished_setsCorrectRestContext() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Ex A", sets = 1, amount = 10, pauseSeconds = 90, orderIndex = 0),
            Exercise(id = 2, workoutId = 1, name = "Ex B", sets = 2, amount = 8, pauseSeconds = 60, orderIndex = 1),
        )
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        val ctx = vm.uiState.value.restContext
        assertNotNull(ctx)
        assertEquals(1, ctx!!.nextExerciseIndex)
        assertEquals(0, ctx.nextSet)
    }

    @Test
    fun onSetFinished_skipsRestForZeroPause() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Ex A", sets = 1, amount = 10, pauseSeconds = 0, orderIndex = 0),
            Exercise(id = 2, workoutId = 1, name = "Ex B", sets = 1, amount = 8, pauseSeconds = 0, orderIndex = 1),
        )
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        assertEquals(ActiveState.EXERCISING, vm.uiState.value.state)
        assertEquals(1, vm.uiState.value.currentExerciseIndex)
        assertEquals(0, vm.uiState.value.completedSets)
    }

    @Test
    fun onSetFinished_doesNothingWhenNotExercising() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.onSetFinished()
        assertEquals(ActiveState.READY, vm.uiState.value.state)
    }

    @Test
    fun onSetFinished_lastSetOfLastExercise_finishesWorkout() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Only", sets = 1, amount = 10, pauseSeconds = 60, orderIndex = 0),
        )
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        assertEquals(ActiveState.FINISHED, vm.uiState.value.state)
    }

    @Test
    fun onSetFinished_lastSetOfLastExercise_recordsCompletion() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Only", sets = 1, amount = 10, pauseSeconds = 60, orderIndex = 0),
        )
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        assertEquals(1, dao.completions.size)
        assertEquals(1L, dao.completions[0].workoutId)
    }

    @Test
    fun onSetFinished_lastSetOfLastExercise_recordsCompletionEvenIfScreenIsLeftImmediately() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Only", sets = 1, amount = 10, pauseSeconds = 60, orderIndex = 0),
        )
        val gate = CompletableDeferred<Unit>()
        dao.completionGate = gate
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        // Leaving the screen clears the ViewModel while the write is still in progress
        vm.viewModelScope.cancel()
        gate.complete(Unit)
        assertEquals(1, dao.completions.size)
    }

    @Test
    fun onSetFinished_lastSetOfLastExercise_incrementsCompletionCount() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Only", sets = 1, amount = 10, pauseSeconds = 60, orderIndex = 0),
        )
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        assertEquals(1, dao.workouts[0].completionCount)
    }

    @Test
    fun pauseWorkout_fromResting_freezesCountdownAndElapsedTime() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        advanceTimeBy(10_001.milliseconds)
        assertEquals(80, vm.uiState.value.remainingSeconds)
        vm.pauseWorkout()
        advanceTimeBy(60_000.milliseconds)
        assertEquals(80, vm.uiState.value.remainingSeconds)
        vm.resumeWorkout()
        advanceTimeBy(5_001.milliseconds)
        assertEquals(75, vm.uiState.value.remainingSeconds)
        assertEquals(15L, vm.uiState.value.elapsedSeconds)
    }

    @Test
    fun restTimer_countsDownVibratesAndAdvancesToNextSet() = runVmTest {
        twoExerciseWorkout()
        val vm = createVm()
        vm.startWorkout()
        vm.onSetFinished()
        advanceTimeBy(87_001.milliseconds)
        assertEquals(3, vm.uiState.value.remainingSeconds)
        assertEquals(1, vibrations)
        advanceTimeBy(3_000.milliseconds)
        assertEquals(ActiveState.EXERCISING, vm.uiState.value.state)
        assertEquals(0, vm.uiState.value.currentExerciseIndex)
        assertEquals(1, vm.uiState.value.completedSets)
        assertNull(vm.uiState.value.restContext)
        assertEquals(3, vibrations)
        assertEquals(90L, vm.uiState.value.elapsedSeconds)
    }

    @Test
    fun restore_whileResting_continuesCountdownFromRealTime() = runVmTest {
        twoExerciseWorkout()
        val handle = SavedStateHandle(mapOf("workoutId" to 1L))
        val first = createVm(handle = handle)
        first.startWorkout()
        first.onSetFinished()
        advanceTimeBy(10_001.milliseconds)
        first.viewModelScope.cancel()
        advanceTimeBy(20_000.milliseconds) // process is dead, real time keeps going

        val vm = createVm(handle = handle)
        assertEquals(ActiveState.RESTING, vm.uiState.value.state)
        assertEquals(60, vm.uiState.value.remainingSeconds)
        assertEquals(0, vm.uiState.value.currentExerciseIndex)
        assertEquals(1, vm.uiState.value.completedSets)
        advanceTimeBy(60_001.milliseconds)
        assertEquals(ActiveState.EXERCISING, vm.uiState.value.state)
        assertEquals(1, vm.uiState.value.completedSets)
        assertEquals(90L, vm.uiState.value.elapsedSeconds)
    }

    @Test
    fun restore_afterRestExpired_advancesToNextSet() = runVmTest {
        twoExerciseWorkout()
        val handle = SavedStateHandle(mapOf("workoutId" to 1L))
        val first = createVm(handle = handle)
        first.startWorkout()
        first.onSetFinished()
        first.viewModelScope.cancel()
        advanceTimeBy(120_000.milliseconds)

        val vm = createVm(handle = handle)
        assertEquals(ActiveState.EXERCISING, vm.uiState.value.state)
        assertEquals(1, vm.uiState.value.completedSets)
        assertNull(vm.uiState.value.restContext)
    }

    @Test
    fun restore_whilePausedDuringRest_staysPausedAndResumesWithRemainingRest() = runVmTest {
        twoExerciseWorkout()
        val handle = SavedStateHandle(mapOf("workoutId" to 1L))
        val first = createVm(handle = handle)
        first.startWorkout()
        first.onSetFinished()
        advanceTimeBy(10_001.milliseconds)
        first.pauseWorkout()
        val vm = recreate(first, handle)
        advanceTimeBy(60_000.milliseconds)

        assertEquals(ActiveState.PAUSED, vm.uiState.value.state)
        assertEquals(80, vm.uiState.value.remainingSeconds)
        assertEquals(10L, vm.uiState.value.elapsedSeconds)
        vm.resumeWorkout()
        assertEquals(ActiveState.RESTING, vm.uiState.value.state)
        assertEquals(80, vm.uiState.value.remainingSeconds)
        advanceTimeBy(5_001.milliseconds)
        assertEquals(75, vm.uiState.value.remainingSeconds)
        assertEquals(15L, vm.uiState.value.elapsedSeconds)
    }

    @Test
    fun restore_whileExercising_keepsProgressAndElapsedTime() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 1, workoutId = 1, name = "Ex A", sets = 1, amount = 10, pauseSeconds = 0, orderIndex = 0),
            Exercise(id = 2, workoutId = 1, name = "Ex B", sets = 2, amount = 8, pauseSeconds = 0, orderIndex = 1),
        )
        val handle = SavedStateHandle(mapOf("workoutId" to 1L))
        val first = createVm(handle = handle)
        first.startWorkout()
        first.onSetFinished()
        val vm = recreate(first, handle)
        advanceTimeBy(30_001.milliseconds)

        assertEquals(ActiveState.EXERCISING, vm.uiState.value.state)
        assertEquals(1, vm.uiState.value.currentExerciseIndex)
        assertEquals(30L, vm.uiState.value.elapsedSeconds)
    }

    @Test
    fun exercises_areSortedByOrderIndex() = runVmTest {
        seedStandardWorkout(
            Exercise(id = 2, workoutId = 1, name = "Second", sets = 1, amount = 10, pauseSeconds = 60, orderIndex = 1),
            Exercise(id = 1, workoutId = 1, name = "First", sets = 1, amount = 10, pauseSeconds = 60, orderIndex = 0),
        )
        val vm = createVm()
        assertEquals("First", vm.uiState.value.exercises[0].name)
        assertEquals("Second", vm.uiState.value.exercises[1].name)
    }
}
