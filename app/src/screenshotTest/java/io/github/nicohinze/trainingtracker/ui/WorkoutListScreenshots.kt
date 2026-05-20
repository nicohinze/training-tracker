package io.github.nicohinze.trainingtracker.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.ui.theme.TrainingTrackerTheme

@PreviewTest
@PreviewLightDark
@Composable
fun WorkoutListPopulatedScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        WorkoutListContent(
            workouts = listOf(
                Workout(id = 1, name = "Push Day", completionCount = 5),
                Workout(id = 2, name = "Pull Day", completionCount = 3),
                Workout(id = 3, name = "Leg Day", completionCount = 0),
            ),
            onEditWorkout = {},
            onStartWorkout = {},
            onAddWorkout = {},
            onDeleteWorkout = {},
            onRenameWorkout = { _, _ -> },
        )
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun WorkoutListEmptyScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        WorkoutListContent(
            workouts = emptyList(),
            onEditWorkout = {},
            onStartWorkout = {},
            onAddWorkout = {},
            onDeleteWorkout = {},
            onRenameWorkout = { _, _ -> },
        )
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun WorkoutCardScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        Surface {
            WorkoutCard(
                workout = Workout(id = 1, name = "Push Day", completionCount = 5),
                onEdit = {},
                onStart = {},
                onDelete = {},
                onRename = {},
            )
        }
    }
}
