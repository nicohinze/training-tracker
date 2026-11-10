package io.github.nicohinze.trainingtracker.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.nicohinze.trainingtracker.WorkoutEditRoute
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.ExerciseType
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutEditViewModel(
    private val dao: WorkoutDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val workoutId: Long = checkNotNull(savedStateHandle.get<Long>(WorkoutEditRoute::workoutId.name)) {
        "workoutId nav argument is required for WorkoutEditViewModel"
    }

    private val _workout = MutableStateFlow<Workout?>(null)
    val workout = _workout.asStateFlow()

    val exercises = dao
        .getExercisesForWorkout(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _workout.value = dao.getWorkout(workoutId)
        }
    }

    fun addExercise(
        name: String,
        sets: Int,
        amount: Int,
        type: ExerciseType,
        intensity: String?,
        pauseSeconds: Int,
    ) {
        viewModelScope.launch {
            dao.addExercise(
                Exercise(
                    workoutId = workoutId,
                    name = name,
                    sets = sets,
                    amount = amount,
                    type = type,
                    intensity = intensity,
                    pauseSeconds = pauseSeconds,
                ),
            )
        }
    }

    fun updateExercise(
        exercise: Exercise,
        name: String,
        sets: Int,
        amount: Int,
        type: ExerciseType,
        intensity: String?,
        pauseSeconds: Int,
    ) {
        viewModelScope.launch {
            dao.updateExercise(
                exercise.copy(
                    name = name,
                    sets = sets,
                    amount = amount,
                    type = type,
                    intensity = intensity,
                    pauseSeconds = pauseSeconds,
                ),
            )
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.deleteExercise(exercise)
        }
    }

    fun moveExercise(from: Int, to: Int) {
        val snapshot = exercises.value
        viewModelScope.launch {
            val mutable = snapshot.toMutableList()
            val item = mutable.removeAt(from)
            mutable.add(to, item)
            dao.reorderExercises(mutable)
        }
    }

    companion object {
        fun factory(dao: WorkoutDao) = viewModelFactory {
            initializer { WorkoutEditViewModel(dao, createSavedStateHandle()) }
        }
    }
}
