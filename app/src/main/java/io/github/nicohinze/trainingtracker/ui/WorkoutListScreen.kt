package io.github.nicohinze.trainingtracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.formatDuration
import io.github.nicohinze.trainingtracker.viewmodel.WorkoutListViewModel

@Composable
fun WorkoutListScreen(
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    viewModel: WorkoutListViewModel = viewModel(),
) {
    val workouts by viewModel.workouts.collectAsState()
    WorkoutListContent(
        workouts = workouts,
        onEditWorkout = onEditWorkout,
        onStartWorkout = onStartWorkout,
        onAddWorkout = { viewModel.addWorkout(it) },
        onDeleteWorkout = { viewModel.deleteWorkout(it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutListContent(
    workouts: List<Workout>,
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onAddWorkout: (String) -> Unit,
    onDeleteWorkout: (Workout) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Workouts") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add workout")
            }
        },
    ) { padding ->
        if (workouts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No workouts yet.\nTap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(workouts, key = { it.id }) { workout ->
                    WorkoutCard(
                        workout = workout,
                        onEdit = { onEditWorkout(workout.id) },
                        onStart = { onStartWorkout(workout.id) },
                        onDelete = { workoutToDelete = workout },
                    )
                }
            }
        }
        if (showAddDialog) {
            AddWorkoutDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    onAddWorkout(name)
                    showAddDialog = false
                },
            )
        }
        workoutToDelete?.let { workout ->
            AlertDialog(
                onDismissRequest = { workoutToDelete = null },
                title = { Text("Delete Workout") },
                text = { Text("Delete \"${workout.name}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteWorkout(workout)
                        workoutToDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { workoutToDelete = null }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: Workout,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Completed: ${workout.completionCount} | Total: ${formatDuration(
                        workout.totalDurationSeconds,
                    )}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onStart) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start workout",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete workout",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Workout") },
        text = {
            AddWorkoutDialogContent(name = name, onNameChange = { name = it })
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
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
private fun AddWorkoutDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Workout name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview("WorkoutList - Populated", showBackground = true)
@Composable
private fun WorkoutListContentPreview() {
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
    )
}

@Preview("WorkoutList - Empty", showBackground = true)
@Composable
private fun WorkoutListContentEmptyPreview() {
    WorkoutListContent(
        workouts = emptyList(),
        onEditWorkout = {},
        onStartWorkout = {},
        onAddWorkout = {},
        onDeleteWorkout = {},
    )
}

@Preview("WorkoutCard", showBackground = true)
@Composable
private fun WorkoutCardPreview() {
    WorkoutCard(
        workout = Workout(id = 1, name = "Push Day", completionCount = 5),
        onEdit = {},
        onStart = {},
        onDelete = {},
    )
}

@Preview("AddWorkoutDialogContent", showBackground = true)
@Composable
private fun AddWorkoutDialogContentPreview() {
    AddWorkoutDialogContent(
        name = "Leg Day",
        onNameChange = {},
    )
}
