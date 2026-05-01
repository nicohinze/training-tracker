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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import io.github.nicohinze.trainingtracker.data.ExerciseType
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
        onAddExercise = { name, sets, amount, type, intensity, pause ->
            viewModel.addExercise(name, sets, amount, type, intensity, pause)
        },
        onUpdateExercise = { exercise, name, sets, amount, type, intensity, pause ->
            viewModel.updateExercise(exercise, name, sets, amount, type, intensity, pause)
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
    onAddExercise: (String, Int, Int, ExerciseType, String?, Int) -> Unit,
    onUpdateExercise: (Exercise, String, Int, Int, ExerciseType, String?, Int) -> Unit,
    onDeleteExercise: (Exercise) -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title — ${exercises.sumOf { it.sets }} sets") },
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
                onConfirm = { name, sets, amount, type, intensity, pause ->
                    onAddExercise(name, sets, amount, type, intensity, pause)
                    showAddDialog = false
                },
            )
        }
        editingExercise?.let { exercise ->
            ExerciseDialog(
                title = "Edit Exercise",
                initialName = exercise.name,
                initialSets = exercise.sets,
                initialAmount = exercise.amount,
                initialType = exercise.type,
                initialIntensity = exercise.intensity.orEmpty(),
                initialPause = exercise.pauseSeconds,
                onDismiss = { editingExercise = null },
                onConfirm = { name, sets, amount, type, intensity, pause ->
                    onUpdateExercise(exercise, name, sets, amount, type, intensity, pause)
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
                    text = buildString {
                        append("${exercise.sets} sets x ")
                        if (exercise.type == ExerciseType.SECONDS) {
                            append("${exercise.amount} s")
                        } else {
                            append("${exercise.amount} reps")
                        }
                        if (!exercise.intensity.isNullOrBlank()) {
                            append(" @ ${exercise.intensity}")
                        }
                        append(" | ${exercise.pauseSeconds} s rest")
                    },
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
    initialAmount: Int = 10,
    initialType: ExerciseType = ExerciseType.REPS,
    initialIntensity: String = "",
    initialPause: Int = 90,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, ExerciseType, String?, Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var sets by remember { mutableStateOf(initialSets.toString()) }
    var amount by remember { mutableStateOf(initialAmount.toString()) }
    var type by remember { mutableStateOf(initialType) }
    var intensity by remember { mutableStateOf(initialIntensity) }
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
                amount = amount,
                onAmountChange = { amount = it.filter { c -> c.isDigit() } },
                type = type,
                onTypeChange = { type = it },
                intensity = intensity,
                onIntensityChange = { intensity = it },
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
                        amount.toIntOrNull() ?: 1,
                        type,
                        intensity.trim().ifBlank { null },
                        pause.toIntOrNull() ?: 0,
                    )
                },
                enabled = name.isNotBlank() && (sets.toIntOrNull() ?: 0) > 0 && (amount.toIntOrNull() ?: 0) > 0,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
    sets: String,
    onSetsChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    type: ExerciseType,
    onTypeChange: (ExerciseType) -> Unit,
    intensity: String,
    onIntensityChange: (String) -> Unit,
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
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ExerciseType.entries.forEachIndexed { index, exerciseType ->
                SegmentedButton(
                    selected = type == exerciseType,
                    onClick = { onTypeChange(exerciseType) },
                    shape = SegmentedButtonDefaults.itemShape(index, ExerciseType.entries.size),
                ) {
                    Text(if (exerciseType == ExerciseType.REPS) "Reps" else "Seconds")
                }
            }
        }
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
                value = amount,
                onValueChange = onAmountChange,
                label = { Text(if (type == ExerciseType.REPS) "Reps" else "Seconds") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = intensity,
            onValueChange = onIntensityChange,
            label = { Text("Intensity") },
            placeholder = { Text("e.g. 20 kg, rings at 110 cm") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
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

@Preview("WorkoutEdit - Empty", showBackground = true)
@Composable
private fun WorkoutEditContentEmptyPreview() {
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

@Preview("ExerciseCard", showBackground = true)
@Composable
private fun ExerciseCardPreview() {
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

@Preview("ExerciseDialogContent - Add", showBackground = true)
@Composable
private fun ExerciseDialogContentAddPreview() {
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
