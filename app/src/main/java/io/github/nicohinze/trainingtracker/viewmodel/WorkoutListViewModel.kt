package io.github.nicohinze.trainingtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nicohinze.trainingtracker.WorkoutApplication
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutJsonConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutListViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val dao = (application as WorkoutApplication).database.workoutDao()

    val workouts = dao
        .getAllWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWorkout(name: String, color: Int) {
        viewModelScope.launch {
            dao.insertWorkout(Workout(name = name, color = color))
        }
    }

    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            dao.deleteWorkout(workout)
        }
    }

    fun resetAllStats() {
        viewModelScope.launch {
            dao.resetAllStats()
        }
    }

    fun updateWorkout(
        workout: Workout,
        newName: String,
        color: Int,
    ) {
        viewModelScope.launch {
            dao.updateWorkout(workout.copy(name = newName, color = color))
        }
    }

    suspend fun importJson(json: String): Int {
        val workoutsWithExercises = WorkoutJsonConverter.fromJson(json)
        for ((workout, exercises) in workoutsWithExercises) {
            val workoutId = dao.insertWorkout(workout)
            for (exercise in exercises) {
                dao.insertExercise(exercise.copy(workoutId = workoutId))
            }
        }
        return workoutsWithExercises.size
    }

    suspend fun exportJson(): String {
        val workoutsWithExercises = dao.getAllWorkoutsWithExercises()
        return WorkoutJsonConverter.toJson(workoutsWithExercises)
    }
}
