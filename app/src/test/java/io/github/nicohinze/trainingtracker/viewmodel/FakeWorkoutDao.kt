package io.github.nicohinze.trainingtracker.viewmodel

import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutCompletion
import io.github.nicohinze.trainingtracker.data.WorkoutDao
import io.github.nicohinze.trainingtracker.data.WorkoutWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWorkoutDao : WorkoutDao {
    val workouts = mutableListOf<Workout>()
    val exercises = mutableListOf<Exercise>()
    val completions = mutableListOf<WorkoutCompletion>()

    private val workoutsFlow = MutableStateFlow<List<Workout>>(emptyList())
    private val completionsFlow = MutableStateFlow<List<WorkoutCompletion>>(emptyList())
    private val exerciseFlows = mutableMapOf<Long, MutableStateFlow<List<Exercise>>>()

    private var nextWorkoutId = 1L
    private var nextExerciseId = 1L
    private var nextCompletionId = 1L

    private fun sortedExercisesFor(workoutId: Long): List<Exercise> = exercises.filter {
        it.workoutId == workoutId
    }.sortedBy { it.orderIndex }

    private fun exercisesFlowFor(workoutId: Long): MutableStateFlow<List<Exercise>> = exerciseFlows.getOrPut(
        workoutId,
    ) {
        MutableStateFlow(sortedExercisesFor(workoutId))
    }

    private fun refreshExercisesFlow(workoutId: Long) {
        exercisesFlowFor(workoutId).value = sortedExercisesFor(workoutId)
    }

    override fun getAllWorkouts(): Flow<List<Workout>> = workoutsFlow

    override suspend fun getWorkout(id: Long): Workout? = workouts.find { it.id == id }

    override suspend fun insertWorkout(workout: Workout): Long {
        val id = if (workout.id == 0L) nextWorkoutId++ else workout.id
        workouts.add(workout.copy(id = id))
        workoutsFlow.value = workouts.toList()
        return id
    }

    override suspend fun updateWorkout(workout: Workout) {
        val idx = workouts.indexOfFirst { it.id == workout.id }
        if (idx >= 0) {
            workouts[idx] = workout
            workoutsFlow.value = workouts.toList()
        }
    }

    override suspend fun deleteWorkout(workout: Workout) {
        workouts.removeIf { it.id == workout.id }
        workoutsFlow.value = workouts.toList()
    }

    override fun getExercisesForWorkout(workoutId: Long): Flow<List<Exercise>> = exercisesFlowFor(workoutId)

    override suspend fun getExerciseListForWorkout(workoutId: Long): List<Exercise> = sortedExercisesFor(workoutId)

    override suspend fun insertExercise(exercise: Exercise): Long {
        val id = if (exercise.id == 0L) nextExerciseId++ else exercise.id
        exercises.add(exercise.copy(id = id))
        refreshExercisesFlow(exercise.workoutId)
        return id
    }

    override suspend fun updateExercise(exercise: Exercise) {
        val idx = exercises.indexOfFirst { it.id == exercise.id }
        if (idx >= 0) {
            exercises[idx] = exercise
            refreshExercisesFlow(exercise.workoutId)
        }
    }

    override suspend fun deleteExercise(exercise: Exercise) {
        exercises.removeIf { it.id == exercise.id }
        refreshExercisesFlow(exercise.workoutId)
    }

    override suspend fun getMaxOrderIndex(workoutId: Long): Int = exercises.filter {
        it.workoutId == workoutId
    }.maxOfOrNull { it.orderIndex } ?: -1

    override suspend fun updateWorkoutStats(workoutId: Long, durationSeconds: Long) {
        val idx = workouts.indexOfFirst { it.id == workoutId }
        if (idx >= 0) {
            val w = workouts[idx]
            workouts[idx] = w.copy(
                completionCount = w.completionCount + 1,
                totalDurationSeconds = w.totalDurationSeconds + durationSeconds,
            )
            workoutsFlow.value = workouts.toList()
        }
    }

    override suspend fun insertCompletion(completion: WorkoutCompletion) {
        val id = if (completion.id == 0L) nextCompletionId++ else completion.id
        completions.add(completion.copy(id = id))
        completionsFlow.value = completions.toList()
    }

    override suspend fun resetWorkoutCounters() {
        for (i in workouts.indices) {
            workouts[i] = workouts[i].copy(completionCount = 0, totalDurationSeconds = 0)
        }
        workoutsFlow.value = workouts.toList()
    }

    override suspend fun deleteAllCompletions() {
        completions.clear()
        completionsFlow.value = emptyList()
    }

    override fun getCompletionsSince(since: Long): Flow<List<WorkoutCompletion>> = completionsFlow

    override suspend fun getWorkoutWithExercises(workoutId: Long): WorkoutWithDetails? {
        val workout = workouts.find { it.id == workoutId } ?: return null
        // Unsorted on purpose: Room's @Relation doesn't guarantee order, so the ViewModel must sort.
        val exs = exercises.filter { it.workoutId == workoutId }
        val comps = completions.filter { it.workoutId == workoutId }
        return WorkoutWithDetails(workout, exs, comps)
    }

    override suspend fun getAllWorkoutsWithExercisesAndCompletions(): List<WorkoutWithDetails> =
        workouts.map { w ->
            WorkoutWithDetails(
                workout = w,
                exercises = exercises.filter { it.workoutId == w.id }.sortedBy { it.orderIndex },
                completions = completions.filter { it.workoutId == w.id },
            )
        }

    fun seed(workout: Workout) {
        workouts.add(workout)
        workoutsFlow.value = workouts.toList()
    }

    fun seed(exercise: Exercise) {
        exercises.add(exercise)
        refreshExercisesFlow(exercise.workoutId)
    }
}
