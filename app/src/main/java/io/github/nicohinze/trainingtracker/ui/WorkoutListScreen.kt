package io.github.nicohinze.trainingtracker.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nicohinze.trainingtracker.data.DEFAULT_WORKOUT_COLOR
import io.github.nicohinze.trainingtracker.data.WORKOUT_COLORS
import io.github.nicohinze.trainingtracker.data.Workout
import io.github.nicohinze.trainingtracker.formatDuration
import io.github.nicohinze.trainingtracker.viewmodel.WorkoutListViewModel
import kotlinx.coroutines.launch

@Composable
fun WorkoutListScreen(
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onActivity: () -> Unit = {},
    viewModel: WorkoutListViewModel = viewModel(),
) {
    val workouts by viewModel.workouts.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = readJsonFromUri(context, uri)
                val count = viewModel.importJson(json)
                Toast.makeText(context, "Imported $count workout(s)", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Import failed: invalid file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.exportJson()
            writeJsonToUri(context, uri, json)
            Toast.makeText(context, "Workouts exported", Toast.LENGTH_SHORT).show()
        }
    }

    WorkoutListContent(
        workouts = workouts,
        onEditWorkout = onEditWorkout,
        onStartWorkout = onStartWorkout,
        onAddWorkout = { name, color -> viewModel.addWorkout(name, color) },
        onDeleteWorkout = { viewModel.deleteWorkout(it) },
        onUpdateWorkout = { workout, newName, newColor -> viewModel.updateWorkout(workout, newName, newColor) },
        onImport = { importLauncher.launch(arrayOf("application/json")) },
        onExport = { exportLauncher.launch("workouts.json") },
        onResetAllStats = { viewModel.resetAllStats() },
        onActivity = onActivity,
    )
}

private fun readJsonFromUri(context: Context, uri: Uri): String {
    return context.contentResolver.openInputStream(uri)!!.use { inputStream ->
        inputStream.bufferedReader().readText()
    }
}

private fun writeJsonToUri(context: Context, uri: Uri, json: String) {
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(json.toByteArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutListContent(
    workouts: List<Workout>,
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onAddWorkout: (String, Int) -> Unit,
    onDeleteWorkout: (Workout) -> Unit,
    onUpdateWorkout: (Workout, String, Int) -> Unit,
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
    onResetAllStats: () -> Unit = {},
    onActivity: () -> Unit = {},
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportExportMenu by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }
    var workoutToEdit by remember { mutableStateOf<Workout?>(null) }

    val totalRuntime = workouts.sumOf { it.totalDurationSeconds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Workouts") },
                actions = {
                    IconButton(onClick = onActivity) {
                        Icon(Icons.Default.DateRange, contentDescription = "Activity graph")
                    }
                    IconButton(onClick = { showImportExportMenu = true }) {
                        Icon(Icons.Default.ImportExport, contentDescription = "Import/Export")
                    }
                    DropdownMenu(
                        expanded = showImportExportMenu,
                        onDismissRequest = { showImportExportMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import workouts") },
                            onClick = {
                                showImportExportMenu = false
                                onImport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export workouts") },
                            onClick = {
                                showImportExportMenu = false
                                onExport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Reset all stats") },
                            onClick = {
                                showImportExportMenu = false
                                showResetStatsDialog = true
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add workout")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (totalRuntime > 0) {
                Text(
                    text = "Total runtime: ${formatDuration(totalRuntime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
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
                        .weight(1f),
                ) {
                    items(workouts, key = { it.id }) { workout ->
                        WorkoutCard(
                            workout = workout,
                            onEdit = { onEditWorkout(workout.id) },
                            onStart = { onStartWorkout(workout.id) },
                            onDelete = { workoutToDelete = workout },
                            onRename = { workoutToEdit = workout },
                        )
                    }
                }
            }
        }
        if (showAddDialog) {
            WorkoutDialog(
                title = "New Workout",
                initialName = "",
                initialColor = DEFAULT_WORKOUT_COLOR,
                confirmLabel = "Create",
                isConfirmEnabled = { _, _ -> true },
                onDismiss = { showAddDialog = false },
                onConfirm = { name, color ->
                    onAddWorkout(name, color)
                    showAddDialog = false
                },
            )
        }
        workoutToEdit?.let { workout ->
            WorkoutDialog(
                title = "Edit Workout",
                initialName = workout.name,
                initialColor = workout.color,
                confirmLabel = "Save",
                isConfirmEnabled = { name, color -> name != workout.name || color != workout.color },
                onDismiss = { workoutToEdit = null },
                onConfirm = { newName, newColor ->
                    onUpdateWorkout(workout, newName, newColor)
                    workoutToEdit = null
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
        if (showResetStatsDialog) {
            AlertDialog(
                onDismissRequest = { showResetStatsDialog = false },
                title = { Text("Reset All Stats") },
                text = { Text("Reset completion counts and total time for all workouts? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        onResetAllStats()
                        showResetStatsDialog = false
                    }) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetStatsDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
internal fun WorkoutCard(
    workout: Workout,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
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
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(workout.color)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Completed: ${workout.completionCount} | Total: ${
                        formatDuration(
                            workout.totalDurationSeconds,
                        )
                    }",
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
            IconButton(onClick = onRename) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit workout",
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
private fun WorkoutDialog(
    title: String,
    initialName: String,
    initialColor: Int,
    confirmLabel: String,
    isConfirmEnabled: (name: String, color: Int) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableIntStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            WorkoutDialogBody(
                name = name,
                onNameChange = { name = it },
                color = color,
                onColorChange = { color = it },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), color) },
                enabled = name.isNotBlank() && isConfirmEnabled(name.trim(), color),
            ) {
                Text(confirmLabel)
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
private fun WorkoutDialogBody(
    name: String,
    onNameChange: (String) -> Unit,
    color: Int,
    onColorChange: (Int) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Workout name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Color",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ColorPickerRow(
            selectedColor = color,
            onColorSelected = onColorChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColorPickerRow(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WORKOUT_COLORS.chunked(5).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (color == selectedColor) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        CircleShape,
                                    )
                                },
                            ).clickable { onColorSelected(color) },
                    )
                }
            }
        }
    }
}

@Preview("WorkoutList - Populated", showBackground = true)
@Composable
private fun WorkoutListContentPreview() {
    WorkoutListContent(
        workouts = listOf(
            Workout(
                id = 1,
                name = "Push Day",
                completionCount = 5,
                totalDurationSeconds = 5 * 60 * 60 + 576,
                color = WORKOUT_COLORS[0],
            ),
            Workout(
                id = 2,
                name = "Pull Day",
                completionCount = 3,
                totalDurationSeconds = 3 * 60 * 60 + 123,
                color = WORKOUT_COLORS[1],
            ),
            Workout(
                id = 3,
                name = "Leg Day",
                completionCount = 0,
                totalDurationSeconds = 0,
                color = WORKOUT_COLORS[2],
            ),
            Workout(
                id = 4,
                name = "All Day",
                completionCount = 999,
                totalDurationSeconds = 999 * 60 * 60 + 9999,
                color = WORKOUT_COLORS[3],
            ),
        ),
        onEditWorkout = {},
        onStartWorkout = {},
        onAddWorkout = { _, _ -> },
        onDeleteWorkout = {},
        onUpdateWorkout = { _, _, _ -> },
    )
}

@Preview("WorkoutList - Empty", showBackground = true)
@Composable
private fun WorkoutListContentEmptyPreview() {
    WorkoutListContent(
        workouts = emptyList(),
        onEditWorkout = {},
        onStartWorkout = {},
        onAddWorkout = { _, _ -> },
        onDeleteWorkout = {},
        onUpdateWorkout = { _, _, _ -> },
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
        onRename = {},
    )
}

@Preview("WorkoutDialog - New", showBackground = true, widthDp = 320)
@Composable
private fun WorkoutDialogNewPreview() {
    WorkoutDialogBody(
        name = "",
        onNameChange = {},
        color = DEFAULT_WORKOUT_COLOR,
        onColorChange = {},
    )
}

@Preview("WorkoutDialog - Edit", showBackground = true, widthDp = 320)
@Composable
private fun WorkoutDialogEditPreview() {
    WorkoutDialogBody(
        name = "Push Day",
        onNameChange = {},
        color = WORKOUT_COLORS[2],
        onColorChange = {},
    )
}

@Preview("ColorPickerRow", showBackground = true, widthDp = 320)
@Composable
private fun ColorPickerRowPreview() {
    ColorPickerRow(
        selectedColor = WORKOUT_COLORS[2],
        onColorSelected = {},
        modifier = Modifier.padding(16.dp),
    )
}
