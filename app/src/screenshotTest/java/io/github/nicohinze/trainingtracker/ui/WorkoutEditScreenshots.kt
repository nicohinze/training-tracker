package io.github.nicohinze.trainingtracker.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.ExerciseType
import io.github.nicohinze.trainingtracker.ui.theme.TrainingTrackerTheme

@PreviewTest
@PreviewLightDark
@Composable
fun WorkoutEditPopulatedScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        WorkoutEditContent(
            title = "Push Day",
            exercises = listOf(
                Exercise(
                    id = 1,
                    workoutId = 1,
                    name = "Bench Press",
                    sets = 3,
                    amount = 10,
                    pauseSeconds = 90,
                    orderIndex = 0,
                ),
                Exercise(
                    id = 2,
                    workoutId = 1,
                    name = "Overhead Press",
                    sets = 3,
                    amount = 8,
                    pauseSeconds = 90,
                    orderIndex = 1,
                ),
                Exercise(
                    id = 3,
                    workoutId = 1,
                    name = "Triceps Dips",
                    sets = 4,
                    amount = 12,
                    pauseSeconds = 60,
                    orderIndex = 2,
                ),
            ),
            onBack = {},
            onAddExercise = { _, _, _, _, _, _ -> },
            onUpdateExercise = { _, _, _, _, _, _, _ -> },
            onDeleteExercise = {},
            onMoveExercise = { _, _ -> },
        )
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun WorkoutEditEmptyScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        WorkoutEditContent(
            title = "New Workout",
            exercises = emptyList(),
            onBack = {},
            onAddExercise = { _, _, _, _, _, _ -> },
            onUpdateExercise = { _, _, _, _, _, _, _ -> },
            onDeleteExercise = {},
            onMoveExercise = { _, _ -> },
        )
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun ExerciseCardScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        Surface {
            ExerciseCard(
                exercise = Exercise(
                    id = 1,
                    workoutId = 1,
                    name = "Bench Press",
                    sets = 3,
                    amount = 10,
                    pauseSeconds = 90,
                    orderIndex = 0,
                ),
                canMoveUp = true,
                canMoveDown = true,
                onMoveUp = {},
                onMoveDown = {},
                onEdit = {},
                onDelete = {},
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun ExerciseDialogContentScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        Surface {
            ExerciseDialogContent(
                name = "Squats",
                onNameChange = {},
                sets = "3",
                onSetsChange = {},
                amount = "10",
                onAmountChange = {},
                type = ExerciseType.REPS,
                onTypeChange = {},
                intensity = "20 kg",
                onIntensityChange = {},
                pause = "90",
                onPauseChange = {},
            )
        }
    }
}
