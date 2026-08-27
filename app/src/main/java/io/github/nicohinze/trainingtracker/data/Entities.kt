package io.github.nicohinze.trainingtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

val WORKOUT_COLORS = listOf(
    0xFF2196F3.toInt(), // Blue
    0xFF4CAF50.toInt(), // Green
    0xFFF44336.toInt(), // Red
    0xFFFF9800.toInt(), // Orange
    0xFF9C27B0.toInt(), // Purple
    0xFF00BCD4.toInt(), // Cyan
    0xFFE91E63.toInt(), // Pink
    0xFF795548.toInt(), // Brown
    0xFF607D8B.toInt(), // Blue Grey
    0xFFFFEB3B.toInt(), // Yellow
)

val DEFAULT_WORKOUT_COLOR = WORKOUT_COLORS[0]

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val completionCount: Int = 0,
    val totalDurationSeconds: Long = 0,
    val color: Int = DEFAULT_WORKOUT_COLOR,
)

enum class ExerciseType {
    REPS,
    SECONDS,
}

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val sets: Int,
    val amount: Int,
    val type: ExerciseType = ExerciseType.REPS,
    val intensity: String? = null,
    val pauseSeconds: Int,
    val orderIndex: Int,
)

@Entity(
    tableName = "workout_completions",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId"), Index("completedAt")],
)
data class WorkoutCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val completedAt: Long,
    val durationSeconds: Long,
)
