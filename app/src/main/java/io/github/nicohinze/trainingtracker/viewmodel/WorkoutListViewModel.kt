package io.github.nicohinze.trainingtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nicohinze.trainingtracker.WorkoutApplication
import io.github.nicohinze.trainingtracker.data.Workout
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

    fun addWorkout(name: String) {
        viewModelScope.launch {
            dao.insertWorkout(Workout(name = name))
        }
    }

    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            dao.deleteWorkout(workout)
        }
    }

    fun renameWorkout(
        workout: Workout,
        newName: String,
    ) {
        viewModelScope.launch {
            dao.updateWorkout(workout.copy(name = newName))
        }
    }
}
