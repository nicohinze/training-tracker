package io.github.nicohinze.trainingtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY name ASC")
    fun getAllWorkouts(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: Long): Workout?

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    fun getExercisesForWorkout(workoutId: Long): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    suspend fun getExerciseListForWorkout(workoutId: Long): List<Exercise>

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM exercises WHERE workoutId = :workoutId")
    suspend fun getMaxOrderIndex(workoutId: Long): Int

    @Query(
        "UPDATE workouts SET completionCount = completionCount + 1, " +
            "totalDurationSeconds = totalDurationSeconds + :durationSeconds WHERE id = :workoutId",
    )
    suspend fun updateWorkoutStats(workoutId: Long, durationSeconds: Long)

    @Insert
    suspend fun insertCompletion(completion: WorkoutCompletion)

    @Transaction
    suspend fun completeWorkout(workoutId: Long, durationSeconds: Long) {
        updateWorkoutStats(workoutId, durationSeconds)
        insertCompletion(
            WorkoutCompletion(
                workoutId = workoutId,
                completedAt = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
            ),
        )
    }

    @Query("UPDATE workouts SET completionCount = 0, totalDurationSeconds = 0")
    suspend fun resetWorkoutCounters()

    @Query("DELETE FROM workout_completions")
    suspend fun deleteAllCompletions()

    @Transaction
    suspend fun resetAllStats() {
        resetWorkoutCounters()
        deleteAllCompletions()
    }

    @Query("SELECT * FROM workout_completions WHERE completedAt >= :since ORDER BY completedAt ASC")
    fun getCompletionsSince(since: Long): Flow<List<WorkoutCompletion>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Long): WorkoutWithDetails?

    @Transaction
    suspend fun addExercise(exercise: Exercise) {
        val maxOrder = getMaxOrderIndex(exercise.workoutId)
        insertExercise(exercise.copy(orderIndex = maxOrder + 1))
    }

    @Transaction
    suspend fun importWorkouts(workoutsWithData: List<WorkoutWithDetails>) {
        for ((workout, exercises, completions) in workoutsWithData) {
            val workoutId = insertWorkout(workout)
            for (exercise in exercises) {
                insertExercise(exercise.copy(workoutId = workoutId))
            }
            for (completion in completions) {
                insertCompletion(completion.copy(workoutId = workoutId))
            }
        }
    }

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY name ASC")
    suspend fun getAllWorkoutsWithExercisesAndCompletions(): List<WorkoutWithDetails>

    @Transaction
    suspend fun reorderExercises(exercises: List<Exercise>) {
        exercises.forEachIndexed { index, exercise ->
            if (exercise.orderIndex != index) {
                updateExercise(exercise.copy(orderIndex = index))
            }
        }
    }
}
