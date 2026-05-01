package io.github.nicohinze.trainingtracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: WorkoutDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.workoutDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetWorkout() = runTest {
        val id = dao.insertWorkout(Workout(name = "Push Day"))
        val workout = dao.getWorkout(id)
        assertNotNull(workout)
        assertEquals("Push Day", workout!!.name)
        assertEquals(0, workout.completionCount)
    }

    @Test
    fun getWorkout_returnsNullForNonexistentId() = runTest {
        assertNull(dao.getWorkout(999))
    }

    @Test
    fun getAllWorkouts_returnsOrderedByName() = runTest {
        dao.insertWorkout(Workout(name = "Push Day"))
        dao.insertWorkout(Workout(name = "Abs"))
        dao.insertWorkout(Workout(name = "Legs"))

        val workouts = dao.getAllWorkouts().first()
        assertEquals(3, workouts.size)
        assertEquals("Abs", workouts[0].name)
        assertEquals("Legs", workouts[1].name)
        assertEquals("Push Day", workouts[2].name)
    }

    @Test
    fun updateWorkout() = runTest {
        val id = dao.insertWorkout(Workout(name = "Old Name"))
        val workout = dao.getWorkout(id)!!
        dao.updateWorkout(workout.copy(name = "New Name"))

        val updated = dao.getWorkout(id)!!
        assertEquals("New Name", updated.name)
    }

    @Test
    fun deleteWorkout() = runTest {
        val id = dao.insertWorkout(Workout(name = "To Delete"))
        val workout = dao.getWorkout(id)!!
        dao.deleteWorkout(workout)
        assertNull(dao.getWorkout(id))
    }

    @Test
    fun completeWorkout() = runTest {
        val id = dao.insertWorkout(Workout(name = "Test"))
        dao.completeWorkout(id, 120)
        dao.completeWorkout(id, 180)
        dao.completeWorkout(id, 60)

        val workout = dao.getWorkout(id)!!
        assertEquals(3, workout.completionCount)
        assertEquals(360L, workout.totalDurationSeconds)
    }

    @Test
    fun insertAndGetExercises() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        dao.insertExercise(
            Exercise(
                workoutId = workoutId,
                name = "Bench Press",
                sets = 3,
                amount = 10,
                pauseSeconds = 90,
                orderIndex = 0,
            ),
        )
        dao.insertExercise(
            Exercise(
                workoutId = workoutId,
                name = "Squats",
                sets = 4,
                amount = 8,
                pauseSeconds = 120,
                orderIndex = 1,
            ),
        )

        val exercises = dao.getExercisesForWorkout(workoutId).first()
        assertEquals(2, exercises.size)
        assertEquals("Bench Press", exercises[0].name)
        assertEquals("Squats", exercises[1].name)
    }

    @Test
    fun getExercisesForWorkout_orderedByOrderIndex() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "Third", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 2),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "First", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 0),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "Second", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 1),
        )

        val exercises = dao.getExercisesForWorkout(workoutId).first()
        assertEquals("First", exercises[0].name)
        assertEquals("Second", exercises[1].name)
        assertEquals("Third", exercises[2].name)
    }

    @Test
    fun getExerciseListForWorkout_returnsSuspendList() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "A", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 0),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "B", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 1),
        )

        val list = dao.getExerciseListForWorkout(workoutId)
        assertEquals(2, list.size)
        assertEquals("A", list[0].name)
        assertEquals("B", list[1].name)
    }

    @Test
    fun updateExercise() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        val exId = dao.insertExercise(
            Exercise(workoutId = workoutId, name = "Old", sets = 3, amount = 10, pauseSeconds = 60, orderIndex = 0),
        )

        val exercises = dao.getExercisesForWorkout(workoutId).first()
        val exercise = exercises.first { it.id == exId }
        dao.updateExercise(exercise.copy(name = "New", sets = 5, amount = 8, pauseSeconds = 120))

        val updated = dao.getExercisesForWorkout(workoutId).first().first { it.id == exId }
        assertEquals("New", updated.name)
        assertEquals(5, updated.sets)
        assertEquals(8, updated.amount)
        assertEquals(120, updated.pauseSeconds)
    }

    @Test
    fun deleteExercise() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "Keep", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 0),
        )
        val deleteId = dao.insertExercise(
            Exercise(workoutId = workoutId, name = "Remove", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 1),
        )

        val toDelete = dao.getExercisesForWorkout(workoutId).first().first { it.id == deleteId }
        dao.deleteExercise(toDelete)

        val remaining = dao.getExercisesForWorkout(workoutId).first()
        assertEquals(1, remaining.size)
        assertEquals("Keep", remaining[0].name)
    }

    @Test
    fun deleteWorkout_cascadesDeleteToExercises() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "A", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 0),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "B", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 1),
        )

        val workout = dao.getWorkout(workoutId)!!
        dao.deleteWorkout(workout)

        val exercises = dao.getExercisesForWorkout(workoutId).first()
        assertTrue(exercises.isEmpty())
    }

    @Test
    fun getMaxOrderIndex_returnsNegativeOneForEmpty() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        assertEquals(-1, dao.getMaxOrderIndex(workoutId))
    }

    @Test
    fun getMaxOrderIndex_returnsHighestIndex() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "A", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 0),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "B", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 5),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "C", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 3),
        )

        assertEquals(5, dao.getMaxOrderIndex(workoutId))
    }

    @Test
    fun reorderExercises_updatesOrderIndices() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Test"))
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "A", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 0),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "B", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 1),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "C", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 2),
        )

        val original = dao.getExerciseListForWorkout(workoutId)
        val reordered = listOf(original[2], original[0], original[1])
        dao.reorderExercises(reordered)

        val result = dao.getExercisesForWorkout(workoutId).first()
        assertEquals("C", result[0].name)
        assertEquals("A", result[1].name)
        assertEquals("B", result[2].name)
    }

    @Test
    fun getWorkoutWithExercises_returnsBothInTransaction() = runTest {
        val workoutId = dao.insertWorkout(Workout(name = "Full Workout"))
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "Ex1", sets = 3, amount = 10, pauseSeconds = 60, orderIndex = 0),
        )
        dao.insertExercise(
            Exercise(workoutId = workoutId, name = "Ex2", sets = 4, amount = 8, pauseSeconds = 90, orderIndex = 1),
        )

        val (workout, exercises) = dao.getWorkoutWithExercises(workoutId)
        assertNotNull(workout)
        assertEquals("Full Workout", workout!!.name)
        assertEquals(2, exercises.size)
        assertEquals("Ex1", exercises[0].name)
        assertEquals("Ex2", exercises[1].name)
    }

    @Test
    fun getWorkoutWithExercises_returnsNullForNonexistentWorkout() = runTest {
        val (workout, exercises) = dao.getWorkoutWithExercises(999)
        assertNull(workout)
        assertTrue(exercises.isEmpty())
    }

    @Test
    fun exercises_areIsolatedPerWorkout() = runTest {
        val id1 = dao.insertWorkout(Workout(name = "Workout 1"))
        val id2 = dao.insertWorkout(Workout(name = "Workout 2"))

        dao.insertExercise(
            Exercise(workoutId = id1, name = "W1-Ex", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 0),
        )
        dao.insertExercise(
            Exercise(workoutId = id2, name = "W2-Ex1", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 0),
        )
        dao.insertExercise(
            Exercise(workoutId = id2, name = "W2-Ex2", sets = 1, amount = 1, pauseSeconds = 0, orderIndex = 1),
        )

        assertEquals(1, dao.getExercisesForWorkout(id1).first().size)
        assertEquals(2, dao.getExercisesForWorkout(id2).first().size)
    }
}
