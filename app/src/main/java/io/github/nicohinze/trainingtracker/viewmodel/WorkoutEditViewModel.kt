package io.github.nicohinze.trainingtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.nicohinze.trainingtracker.WorkoutApplication
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.ExerciseType
import io.github.nicohinze.trainingtracker.data.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val dao = (application as WorkoutApplication).database.workoutDao()
    private val workoutId: Long = savedStateHandle.get<Long>("workoutId") ?: 0L

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
            val maxOrder = dao.getMaxOrderIndex(workoutId)
            dao.insertExercise(
                Exercise(
                    workoutId = workoutId,
                    name = name,
                    sets = sets,
                    amount = amount,
                    type = type,
                    intensity = intensity,
                    pauseSeconds = pauseSeconds,
                    orderIndex = maxOrder + 1,
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

    fun moveExercise(
        from: Int,
        to: Int,
        currentList: List<Exercise>,
    ) {
        viewModelScope.launch {
            val mutable = currentList.toMutableList()
            val item = mutable.removeAt(from)
            mutable.add(to, item)
            dao.reorderExercises(mutable)
        }
    }
}
