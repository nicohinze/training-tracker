package io.github.nicohinze.trainingtracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import io.github.nicohinze.trainingtracker.data.WORKOUT_COLORS
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutCompletion
import io.github.nicohinze.trainingtracker.ui.theme.TrainingTrackerTheme
import io.github.nicohinze.trainingtracker.viewmodel.ActivityGraphUiState
import java.time.LocalDate
import java.time.ZoneOffset

// Fixed Sunday so the last column is always fully visible in the screenshot.
private val SCREENSHOT_TODAY = LocalDate.of(2026, 8, 30)
private val SCREENSHOT_NOW_MS = SCREENSHOT_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private val sampleWorkouts = listOf(
    Workout(id = 1, name = "Push Day", color = WORKOUT_COLORS[0]),
    Workout(id = 2, name = "Pull Day", color = WORKOUT_COLORS[1]),
    Workout(id = 3, name = "Leg Day", color = WORKOUT_COLORS[2]),
)

private fun sampleCompletions(): List<WorkoutCompletion> {
    val day = 24L * 60 * 60 * 1000
    return buildList {
        for (weeksAgo in 0..8) {
            val workoutId = (weeksAgo % 3 + 1).toLong()
            add(
                WorkoutCompletion(
                    id = weeksAgo.toLong(),
                    workoutId = workoutId,
                    completedAt = SCREENSHOT_NOW_MS - 7 * weeksAgo * day - (weeksAgo % 3) * day,
                    durationSeconds = 3600,
                ),
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun ActivityGraphPopulatedScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        ActivityGraphContent(
            uiState = ActivityGraphUiState(
                completions = sampleCompletions(),
                workouts = sampleWorkouts,
            ),
            onBack = {},
            today = SCREENSHOT_TODAY,
        )
    }
}

@PreviewTest
@PreviewLightDark
@Composable
fun ActivityGraphEmptyScreenshot() {
    TrainingTrackerTheme(dynamicColor = false) {
        ActivityGraphContent(
            uiState = ActivityGraphUiState(),
            onBack = {},
            today = SCREENSHOT_TODAY,
        )
    }
}
