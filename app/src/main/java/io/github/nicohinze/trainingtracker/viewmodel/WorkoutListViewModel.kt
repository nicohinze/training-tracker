package io.github.nicohinze.trainingtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutDao
import io.github.nicohinze.trainingtracker.data.WorkoutJsonConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutListViewModel(
    private val dao: WorkoutDao,
) : ViewModel() {
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

    suspend fun importJson(json: String): Result<Int> = runCatching {
        val workoutsWithData = WorkoutJsonConverter.fromJson(json)
        dao.importWorkouts(workoutsWithData)
        workoutsWithData.size
    }

    suspend fun exportJson(): String {
        val workoutsWithData = dao.getAllWorkoutsWithExercisesAndCompletions()
        return WorkoutJsonConverter.toJson(workoutsWithData)
    }

    companion object {
        fun factory(dao: WorkoutDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = WorkoutListViewModel(dao) as T
        }
    }
}
