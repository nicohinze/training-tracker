package io.github.nicohinze.trainingtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nicohinze.trainingtracker.data.Exercise
import io.github.nicohinze.trainingtracker.viewmodel.WorkoutEditViewModel

@Composable
fun WorkoutEditScreen(
    onBack: () -> Unit,
    viewModel: WorkoutEditViewModel = viewModel(),
) {
    val workout by viewModel.workout.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    WorkoutEditContent(
        title = workout?.name ?: "Edit Workout",
        exercises = exercises,
        onBack = onBack,
        onAddExercise = { name, sets, reps, pause -> viewModel.addExercise(name, sets, reps, pause) },
        onUpdateExercise = { exercise, name, sets, reps, pause ->
            viewModel.updateExercise(exercise, name, sets, reps, pause)
        },
        onDeleteExercise = { viewModel.deleteExercise(it) },
        onMoveExercise = { from, to -> viewModel.moveExercise(from, to, exercises) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutEditContent(
    title: String,
    exercises: List<Exercise>,
    onBack: () -> Unit,
    onAddExercise: (String, Int, Int, Int) -> Unit,
    onUpdateExercise: (Exercise, String, Int, Int, Int) -> Unit,
    onDeleteExercise: (Exercise) -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            itemsIndexed(exercises, key = { _, ex -> ex.id }) { index, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    canMoveUp = index > 0,
                    canMoveDown = index < exercises.size - 1,
                    onMoveUp = { onMoveExercise(index, index - 1) },
                    onMoveDown = { onMoveExercise(index, index + 1) },
                    onEdit = { editingExercise = exercise },
                    onDelete = { exerciseToDelete = exercise },
                )
            }
        }
        if (showAddDialog) {
            ExerciseDialog(
                title = "Add Exercise",
                onDismiss = { showAddDialog = false },
                onConfirm = { name, sets, reps, pause ->
                    onAddExercise(name, sets, reps, pause)
                    showAddDialog = false
                },
            )
        }
        editingExercise?.let { exercise ->
            ExerciseDialog(
                title = "Edit Exercise",
                initialName = exercise.name,
                initialSets = exercise.sets,
                initialReps = exercise.reps,
                initialPause = exercise.pauseSeconds,
                onDismiss = { editingExercise = null },
                onConfirm = { name, sets, reps, pause ->
                    onUpdateExercise(exercise, name, sets, reps, pause)
                    editingExercise = null
                },
            )
        }
        exerciseToDelete?.let { exercise ->
            AlertDialog(
                onDismissRequest = { exerciseToDelete = null },
                title = { Text("Delete Exercise") },
                text = { Text("Delete \"${exercise.name}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteExercise(exercise)
                        exerciseToDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exerciseToDelete = null }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${exercise.sets} sets x ${exercise.reps} reps | ${exercise.pauseSeconds}s rest",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ExerciseDialog(
    title: String,
    initialName: String = "",
    initialSets: Int = 3,
    initialReps: Int = 10,
    initialPause: Int = 90,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var sets by remember { mutableStateOf(initialSets.toString()) }
    var reps by remember { mutableStateOf(initialReps.toString()) }
    var pause by remember { mutableStateOf(initialPause.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            ExerciseDialogContent(
                name = name,
                onNameChange = { name = it },
                sets = sets,
                onSetsChange = { sets = it.filter { c -> c.isDigit() } },
                reps = reps,
                onRepsChange = { reps = it.filter { c -> c.isDigit() } },
                pause = pause,
                onPauseChange = { pause = it.filter { c -> c.isDigit() } },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name.trim(),
                        sets.toIntOrNull() ?: 1,
                        reps.toIntOrNull() ?: 1,
                        pause.toIntOrNull() ?: 0,
                    )
                },
                enabled = name.isNotBlank() && (sets.toIntOrNull() ?: 0) > 0 && (reps.toIntOrNull() ?: 0) > 0,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ExerciseDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
    sets: String,
    onSetsChange: (String) -> Unit,
    reps: String,
    onRepsChange: (String) -> Unit,
    pause: String,
    onPauseChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Exercise name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = sets,
                onValueChange = onSetsChange,
                label = { Text("Sets") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = reps,
                onValueChange = onRepsChange,
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pause,
            onValueChange = onPauseChange,
            label = { Text("Rest between sets (seconds)") },
            placeholder = { Text("0") },
            supportingText = if (pause.isBlank() || pause == "0") {
                { Text("No rest between sets") }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview("WorkoutEdit - Populated", showBackground = true)
@Composable
private fun WorkoutEditContentPreview() {
    WorkoutEditContent(
        title = "Push Day",
        exercises = listOf(
            Exercise(
                id = 1,
                workoutId = 1,
                name = "Bench Press",
                sets = 3,
                reps = 10,
                pauseSeconds = 90,
                orderIndex = 0,
            ),
            Exercise(
                id = 2,
                workoutId = 1,
                name = "Overhead Press",
                sets = 3,
                reps = 8,
                pauseSeconds = 90,
                orderIndex = 1,
            ),
            Exercise(
                id = 3,
                workoutId = 1,
                name = "Tricep Dips",
                sets = 4,
                reps = 12,
                pauseSeconds = 60,
                orderIndex = 2,
            ),
        ),
        onBack = {},
        onAddExercise = { _, _, _, _ -> },
        onUpdateExercise = { _, _, _, _, _ -> },
        onDeleteExercise = {},
        onMoveExercise = { _, _ -> },
    )
}

@Preview("WorkoutEdit - Empty", showBackground = true)
@Composable
private fun WorkoutEditContentEmptyPreview() {
    WorkoutEditContent(
        title = "New Workout",
        exercises = emptyList(),
        onBack = {},
        onAddExercise = { _, _, _, _ -> },
        onUpdateExercise = { _, _, _, _, _ -> },
        onDeleteExercise = {},
        onMoveExercise = { _, _ -> },
    )
}

@Preview("ExerciseCard", showBackground = true)
@Composable
private fun ExerciseCardPreview() {
    ExerciseCard(
        exercise = Exercise(
            id = 1,
            workoutId = 1,
            name = "Bench Press",
            sets = 3,
            reps = 10,
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

@Preview("ExerciseDialogContent - Add", showBackground = true)
@Composable
private fun ExerciseDialogContentAddPreview() {
    ExerciseDialogContent(
        name = "Squats",
        onNameChange = {},
        sets = "3",
        onSetsChange = {},
        reps = "10",
        onRepsChange = {},
        pause = "90",
        onPauseChange = {},
    )
}
