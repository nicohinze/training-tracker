package io.github.nicohinze.trainingtracker

import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.nicohinze.trainingtracker.data.WorkoutDao
import io.github.nicohinze.trainingtracker.ui.AboutScreen
import io.github.nicohinze.trainingtracker.ui.ActiveWorkoutScreen
import io.github.nicohinze.trainingtracker.ui.ActivityGraphScreen
import io.github.nicohinze.trainingtracker.ui.WorkoutEditScreen
import io.github.nicohinze.trainingtracker.ui.WorkoutListScreen
import io.github.nicohinze.trainingtracker.ui.theme.TrainingTrackerTheme
import io.github.nicohinze.trainingtracker.viewmodel.ActiveWorkoutViewModel
import io.github.nicohinze.trainingtracker.viewmodel.ActivityGraphViewModel
import io.github.nicohinze.trainingtracker.viewmodel.WorkoutEditViewModel
import io.github.nicohinze.trainingtracker.viewmodel.WorkoutListViewModel

private const val VIBRATION_DURATION_MS = 100L

@Composable
private fun AppNavHost(navController: NavHostController, dao: WorkoutDao, onVibrate: () -> Unit) {
    NavHost(navController = navController, startDestination = WorkoutListRoute) {
        composable<WorkoutListRoute> {
            WorkoutListScreen(
                onEditWorkout = { id -> navController.navigate(WorkoutEditRoute(id)) },
                onStartWorkout = { id -> navController.navigate(ActiveWorkoutRoute(id)) },
                onAbout = { navController.navigate(AboutRoute) },
                onActivity = { navController.navigate(ActivityGraphRoute) },
                factory = WorkoutListViewModel.factory(dao),
            )
        }
        composable<AboutRoute> {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable<ActivityGraphRoute> {
            ActivityGraphScreen(
                onBack = { navController.popBackStack() },
                factory = ActivityGraphViewModel.factory(dao),
            )
        }
        composable<WorkoutEditRoute> {
            WorkoutEditScreen(
                onBack = { navController.popBackStack() },
                factory = WorkoutEditViewModel.factory(dao),
            )
        }
        composable<ActiveWorkoutRoute> {
            ActiveWorkoutScreen(
                onBack = { navController.popBackStack() },
                factory = ActiveWorkoutViewModel.factory(dao, onVibrate),
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = (application as WorkoutApplication).database.workoutDao()
        // Application context: the lambda is retained by ActiveWorkoutViewModel, which outlives this Activity.
        val vibrator = applicationContext.getSystemService(VibratorManager::class.java).defaultVibrator
        val onVibrate = {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        setContent {
            TrainingTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController, dao = dao, onVibrate = onVibrate)
                }
            }
        }
    }
}
