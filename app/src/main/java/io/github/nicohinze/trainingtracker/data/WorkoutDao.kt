package io.github.nicohinze.trainingtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    @Query("UPDATE workouts SET completionCount = completionCount + 1 WHERE id = :workoutId")
    suspend fun incrementCompletionCount(workoutId: Long)
}
