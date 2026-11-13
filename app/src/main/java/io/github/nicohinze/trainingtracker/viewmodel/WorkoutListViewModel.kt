package io.github.nicohinze.trainingtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutDao
import io.github.nicohinze.trainingtracker.data.WorkoutJsonConverter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class WorkoutListViewModel(
    private val dao: WorkoutDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val workouts = dao
        .getAllWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Buffered, so a result that arrives while the screen is being recreated is shown once it is back.
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

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

    /** Runs in [viewModelScope], so the import isn't cancelled by e.g. a screen rotation. */
    fun importWorkouts(readJson: suspend () -> String) {
        viewModelScope.launch {
            val message = try {
                val json = withContext(ioDispatcher) { readJson() }
                importJson(json).fold(
                    onSuccess = { count -> "Imported $count workout(s)" },
                    onFailure = { "Import failed: invalid file" },
                )
            } catch (_: IOException) {
                "Import failed: could not read file"
            } catch (_: SecurityException) {
                "Import failed: no permission to read file"
            }
            _messages.send(message)
        }
    }

    /** Runs in [viewModelScope], so the export isn't cancelled by e.g. a screen rotation. */
    fun exportWorkouts(writeJson: suspend (String) -> Unit) {
        viewModelScope.launch {
            val json = exportJson()
            val message = try {
                withContext(ioDispatcher) { writeJson(json) }
                "Workouts exported"
            } catch (_: IOException) {
                "Export failed: could not write file"
            } catch (_: SecurityException) {
                "Export failed: no permission to write file"
            }
            _messages.send(message)
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
        fun factory(dao: WorkoutDao) = viewModelFactory {
            initializer { WorkoutListViewModel(dao) }
        }
    }
}
