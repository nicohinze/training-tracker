package io.github.nicohinze.trainingtracker.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.ExerciseType
import io.github.nicohinze.trainingtracker.ui.theme.TrainingTrackerTheme

private val sampleExercises = listOf(
    Exercise(
        id = 1,
        workoutId = 1,
        name = "Bench Press",
        sets = 3,
        amount = 10,
        intensity = "50 kg",
        pauseSeconds = 90,
        orderIndex = 0,
    ),
    Exercise(
        id = 2,
        workoutId = 1,
        name = "Squats",
        sets = 4,
        amount = 8,
        pauseSeconds = 120,
        orderIndex = 1,
    ),
    Exercise(
        id = 3,
        workoutId = 1,
        name = "Pull-ups",
        sets = 3,
        amount = 12,
        pauseSeconds = 60,
        orderIndex = 2,
    ),
    Exercise(
        id = 4,
        workoutId = 1,
        name = "Plank",
        sets = 2,
        amount = 12,
        type = ExerciseType.SECONDS,
        intensity = "10 kg",
        pauseSeconds = 60,
        orderIndex = 3,
    ),
    Exercise(
        id = 5,
        workoutId = 1,
        name = "Pull-ups",
        sets = 1,
        amount = 12,
        pauseSeconds = 60,
        orderIndex = 4,
    ),
)

@PreviewTest
@PreviewLightDark
@Composable
fun ReadyContentScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        Surface {
            ReadyContent(
                exercises = sampleExercises,
                onStart = {},
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun ExercisingContentScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        Surface {
            ExercisingContent(
                exercises = sampleExercises,
                currentExerciseIndex = 3,
                completedSets = 1,
                onSetDone = {},
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun RestingContentScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        Surface {
            RestingContent(
                exercises = sampleExercises,
                currentExerciseIndex = 3,
                completedSets = 1,
                remainingSeconds = 7,
                totalPauseSeconds = 10,
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun FinishedContentScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        Surface {
            FinishedContent(1 * 60 * 60 + 9 * 60 + 9) {}
        }
    }
}
