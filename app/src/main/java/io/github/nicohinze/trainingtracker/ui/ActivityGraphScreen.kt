package io.github.nicohinze.trainingtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nicohinze.trainingtracker.data.WORKOUT_COLORS
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.data.WorkoutCompletion
import io.github.nicohinze.trainingtracker.viewmodel.ActivityGraphUiState
import io.github.nicohinze.trainingtracker.viewmodel.ActivityGraphViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

private val CELL_SIZE = 12.dp
private val CELL_GAP = 2.dp
private val CELL_RADIUS = 2.dp
private val MONTH_LABEL_HEIGHT = 14.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityGraphScreen(
    onBack: () -> Unit,
    viewModel: ActivityGraphViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ActivityGraphContent(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityGraphContent(
    uiState: ActivityGraphUiState,
    onBack: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val startOfCurrentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val startDate = startOfCurrentWeek.minusWeeks(51)
    val workoutMap = uiState.workouts.associateBy { it.id }
    val dayColorMap = buildMap<LocalDate, Int> {
        for (completion in uiState.completions.sortedBy { it.completedAt }) {
            val date = Instant
                .ofEpochMilli(completion.completedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            if (!containsKey(date)) {
                workoutMap[completion.workoutId]?.color?.let { put(date, it) }
            }
        }
    }
    val activeWorkoutIds = uiState.completions.map { it.workoutId }.toSet()
    val activeWorkouts = uiState.workouts.filter { it.id in activeWorkoutIds }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Last 52 weeks",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                DayLabels()
                Spacer(Modifier.width(4.dp))
                ActivityCalendar(startDate = startDate, today = today, dayColorMap = dayColorMap)
            }
            Spacer(Modifier.height(24.dp))
            WorkoutLegend(activeWorkouts = activeWorkouts)
        }
    }
}

@Composable
private fun DayLabels() {
    Column(modifier = Modifier.padding(top = MONTH_LABEL_HEIGHT)) {
        val dayLabels = listOf("M", "", "W", "", "F", "", "S")
        dayLabels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier.size(width = 10.dp, height = CELL_SIZE),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (index < 6) {
                Spacer(Modifier.height(CELL_GAP))
            }
        }
    }
}

@Composable
private fun ActivityCalendar(
    startDate: LocalDate,
    today: LocalDate,
    dayColorMap: Map<LocalDate, Int>,
) {
    Column(modifier = Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE))) {
        Row {
            for (weekIndex in 0 until 52) {
                val monday = startDate.plusWeeks(weekIndex.toLong())
                val prevMonday = if (weekIndex > 0) startDate.plusWeeks((weekIndex - 1).toLong()) else null
                val showMonth = prevMonday == null || monday.month != prevMonday.month
                Box(
                    modifier = Modifier
                        .width(CELL_SIZE)
                        .height(MONTH_LABEL_HEIGHT),
                ) {
                    if (showMonth) {
                        Text(
                            text = monday.month.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (weekIndex < 51) {
                    Spacer(Modifier.width(CELL_GAP))
                }
            }
        }
        WeekGrid(startDate = startDate, today = today, dayColorMap = dayColorMap)
    }
}

@Composable
private fun WeekGrid(
    startDate: LocalDate,
    today: LocalDate,
    dayColorMap: Map<LocalDate, Int>,
) {
    Row {
        for (weekIndex in 0 until 52) {
            Column {
                for (dayOfWeek in 0 until 7) {
                    val date = startDate.plusDays(7L * weekIndex + dayOfWeek)
                    val isFuture = date.isAfter(today)
                    val colorInt = if (!isFuture) dayColorMap[date] else null
                    val bgColor = when {
                        isFuture -> Color.Transparent
                        colorInt != null -> Color(colorInt)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(CELL_SIZE)
                            .clip(RoundedCornerShape(CELL_RADIUS))
                            .background(bgColor),
                    )
                    if (dayOfWeek < 6) {
                        Spacer(Modifier.height(CELL_GAP))
                    }
                }
            }
            if (weekIndex < 51) {
                Spacer(Modifier.width(CELL_GAP))
            }
        }
    }
}

@Composable
private fun WorkoutLegend(activeWorkouts: List<Workout>) {
    if (activeWorkouts.isEmpty()) {
        Text(
            text = "Complete a workout to see your activity here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = "Legend",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        activeWorkouts.forEach { workout ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(workout.color)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview("ActivityGraph - with data", showBackground = true)
@Composable
private fun ActivityGraphContentPreview() {
    val now = System.currentTimeMillis()
    val day = 24L * 60 * 60 * 1000
    val workouts = listOf(
        Workout(id = 1, name = "Push Day", color = WORKOUT_COLORS[0]),
        Workout(id = 2, name = "Pull Day", color = WORKOUT_COLORS[1]),
        Workout(id = 3, name = "Leg Day", color = WORKOUT_COLORS[2]),
    )
    val completions = buildList {
        for (weeksAgo in 0..8) {
            val workoutId = (weeksAgo % 3 + 1).toLong()
            add(
                WorkoutCompletion(
                    id = weeksAgo.toLong(),
                    workoutId = workoutId,
                    completedAt = now - 7 * weeksAgo * day - (weeksAgo % 3) * day,
                    durationSeconds = 3600,
                ),
            )
        }
    }
    ActivityGraphContent(
        uiState = ActivityGraphUiState(
            completions = completions,
            workouts = workouts,
        ),
        onBack = {},
    )
}

@Preview("ActivityGraph - empty", showBackground = true)
@Composable
private fun ActivityGraphContentEmptyPreview() {
    ActivityGraphContent(
        uiState = ActivityGraphUiState(),
        onBack = {},
    )
}
