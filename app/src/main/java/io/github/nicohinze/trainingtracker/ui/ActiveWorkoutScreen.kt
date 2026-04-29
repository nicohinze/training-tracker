package io.github.nicohinze.trainingtracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.data.ExerciseType
import io.github.nicohinze.trainingtracker.viewmodel.ActiveState
import io.github.nicohinze.trainingtracker.viewmodel.ActiveWorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onBack: () -> Unit,
    viewModel: ActiveWorkoutViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQuitDialog by remember { mutableStateOf(false) }
    BackHandler {
        if (uiState.state == ActiveState.EXERCISING || uiState.state == ActiveState.RESTING) {
            showQuitDialog = true
        } else {
            onBack()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.workout?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.state == ActiveState.EXERCISING || uiState.state == ActiveState.RESTING) {
                            showQuitDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = uiState.state,
                label = "workout_state",
            ) { state ->
                when (state) {
                    ActiveState.READY -> {
                        ReadyContent(
                            exercises = uiState.exercises,
                            onStart = { viewModel.startWorkout() },
                        )
                    }

                    ActiveState.EXERCISING -> {
                        ExercisingContent(
                            exercises = uiState.exercises,
                            currentExerciseIndex = uiState.currentExerciseIndex,
                            completedSets = uiState.completedSets,
                            onSetDone = { viewModel.onSetFinished() },
                        )
                    }

                    ActiveState.RESTING -> {
                        RestingContent(
                            exercises = uiState.exercises,
                            currentExerciseIndex = uiState.currentExerciseIndex,
                            completedSets = uiState.completedSets,
                            remainingSeconds = uiState.remainingSeconds,
                            totalPauseSeconds = uiState.currentExercise?.pauseSeconds ?: 1,
                        )
                    }

                    ActiveState.FINISHED -> {
                        FinishedContent(onDone = onBack)
                    }
                }
            }
        }
        if (showQuitDialog) {
            AlertDialog(
                onDismissRequest = { showQuitDialog = false },
                title = { Text("Quit Workout?") },
                text = { Text("Your progress will be lost.") },
                confirmButton = {
                    TextButton(onClick = onBack) {
                        Text("Quit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuitDialog = false }) {
                        Text("Continue")
                    }
                },
            )
        }
    }
}

@Composable
private fun ReadyContent(
    exercises: List<Exercise>,
    onStart: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp),
    ) {
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "This workout has no exercises.\nGo back and add some first.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            Text(
                "${exercises.size} exercises — ${exercises.sumOf { it.sets }} sets",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            TableHeader()
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(exercises) { index, exercise ->
                    ExerciseRow(
                        index = index,
                        exercise = exercise,
                        setsDisplay = "${exercise.sets}",
                    )
                    if (index < exercises.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(64.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Start Workout", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ExercisingContent(
    exercises: List<Exercise>,
    currentExerciseIndex: Int,
    completedSets: Int,
    onSetDone: () -> Unit,
) {
    ExerciseTable(exercises, currentExerciseIndex, completedSets) {
        Button(
            onClick = onSetDone,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Set Done", fontSize = 18.sp)
        }
    }
}

@Composable
private fun RestingContent(
    exercises: List<Exercise>,
    currentExerciseIndex: Int,
    completedSets: Int,
    remainingSeconds: Int,
    totalPauseSeconds: Int,
) {
    val progress by animateFloatAsState(
        targetValue = if (totalPauseSeconds > 0) remainingSeconds.toFloat() / totalPauseSeconds else 0f,
        label = "rest_progress",
    )
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    ExerciseTable(exercises, currentExerciseIndex, completedSets) {
        Text(
            text = "Rest",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(150.dp),
                strokeWidth = 10.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = if (minutes > 0) "%d:%02d".format(minutes, seconds) else "$seconds",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FinishedContent(onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp),
    ) {
        Text(
            text = "Workout Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
        ) {
            Text("Done", fontSize = 18.sp)
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "#",
            modifier = Modifier.weight(0.5f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Exercise",
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Sets",
            modifier = Modifier.weight(0.7f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Reps/Seconds",
            modifier = Modifier.weight(0.7f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Pause",
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

@Composable
private fun ExerciseRow(
    index: Int,
    exercise: Exercise,
    setsDisplay: String,
    highlighted: Boolean = false,
) {
    val rowModifier = if (highlighted) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    }
    val contentColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}",
            modifier = Modifier.weight(0.5f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlighted) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            exercise.name,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
        )
        Text(
            setsDisplay,
            modifier = Modifier.weight(0.7f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = contentColor,
        )
        Text(
            if (exercise.type == ExerciseType.SECONDS) "${exercise.amount} s" else "${exercise.amount}",
            modifier = Modifier.weight(0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = contentColor,
        )
        Text(
            "${exercise.pauseSeconds} s",
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = contentColor,
        )
    }
}

@Composable
private fun ExerciseTable(
    exercises: List<Exercise>,
    currentExerciseIndex: Int,
    completedSets: Int,
    bottomContent: @Composable () -> Unit = {},
) {
    val currentExercise = exercises[currentExerciseIndex]
    val listState = rememberLazyListState()
    LaunchedEffect(currentExerciseIndex) {
        listState.animateScrollToItem(currentExerciseIndex)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp),
    ) {
        Text(
            currentExercise.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            buildString {
                append("Set $completedSets/${currentExercise.sets} — ")
                if (currentExercise.type == ExerciseType.SECONDS) {
                    append("${currentExercise.amount} s")
                } else {
                    append("${currentExercise.amount} reps")
                }
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        TableHeader()
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
        ) {
            itemsIndexed(exercises) { index, exercise ->
                val isCurrent = index == currentExerciseIndex
                val setsDisplay = when {
                    index < currentExerciseIndex -> "${exercise.sets}/${exercise.sets}"
                    isCurrent -> "$completedSets/${exercise.sets}"
                    else -> "0/${exercise.sets}"
                }
                ExerciseRow(
                    index = index,
                    exercise = exercise,
                    setsDisplay = setsDisplay,
                    highlighted = isCurrent,
                )
                if (index < exercises.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        bottomContent()
    }
}

@Preview("ReadyContent", showBackground = true)
@Composable
private fun ReadyContentPreview() {
    ReadyContent(
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
        ),
        onStart = {},
    )
}

@Preview("ExercisingContentReps", showBackground = true)
@Composable
private fun ExercisingContentRepsPreview() {
    val exercises = listOf(
        Exercise(
            id = 1,
            workoutId = 1,
            name = "Bench Press",
            sets = 3,
            amount = 10,
            pauseSeconds = 90,
            orderIndex = 0,
        ),
        Exercise(id = 2, workoutId = 1, name = "Squats", sets = 4, amount = 8, pauseSeconds = 120, orderIndex = 1),
        Exercise(id = 3, workoutId = 1, name = "Pull-ups", sets = 3, amount = 12, pauseSeconds = 60, orderIndex = 2),
        Exercise(
            id = 4,
            workoutId = 1,
            name = "Plank",
            sets = 2,
            amount = 12,
            type = ExerciseType.SECONDS,
            pauseSeconds = 60,
            orderIndex = 3,
        ),
        Exercise(id = 5, workoutId = 1, name = "Pull-ups", sets = 1, amount = 12, pauseSeconds = 60, orderIndex = 4),
    )
    ExercisingContent(
        exercises = exercises,
        currentExerciseIndex = 1,
        completedSets = 2,
        onSetDone = {},
    )
}

@Preview("ExercisingContentSeconds", showBackground = true)
@Composable
private fun ExercisingContentSecondsPreview() {
    val exercises = listOf(
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
            name = "Plank",
            sets = 4,
            amount = 8,
            type = ExerciseType.SECONDS,
            pauseSeconds = 120,
            orderIndex = 1,
        ),
        Exercise(id = 3, workoutId = 1, name = "Pull-ups", sets = 3, amount = 12, pauseSeconds = 60, orderIndex = 2),
        Exercise(
            id = 4,
            workoutId = 1,
            name = "Plank",
            sets = 2,
            amount = 12,
            type = ExerciseType.SECONDS,
            pauseSeconds = 60,
            orderIndex = 3,
        ),
        Exercise(id = 5, workoutId = 1, name = "Pull-ups", sets = 1, amount = 12, pauseSeconds = 60, orderIndex = 4),
    )
    ExercisingContent(
        exercises = exercises,
        currentExerciseIndex = 1,
        completedSets = 2,
        onSetDone = {},
    )
}

@Preview("RestingContent", showBackground = true)
@Composable
private fun RestingContentPreview() {
    val exercises = listOf(
        Exercise(
            id = 1,
            workoutId = 1,
            name = "Bench Press",
            sets = 3,
            amount = 10,
            pauseSeconds = 90,
            orderIndex = 0,
        ),
        Exercise(id = 2, workoutId = 1, name = "Squats", sets = 4, amount = 8, pauseSeconds = 120, orderIndex = 1),
        Exercise(id = 3, workoutId = 1, name = "Pull-ups", sets = 3, amount = 12, pauseSeconds = 60, orderIndex = 2),
        Exercise(
            id = 4,
            workoutId = 1,
            name = "Plank",
            sets = 2,
            amount = 12,
            type = ExerciseType.SECONDS,
            pauseSeconds = 60,
            orderIndex = 3,
        ),
        Exercise(id = 5, workoutId = 1, name = "Pull-ups", sets = 1, amount = 12, pauseSeconds = 60, orderIndex = 4),
    )
    RestingContent(
        exercises = exercises,
        currentExerciseIndex = 1,
        completedSets = 2,
        7,
        10,
    )
}
