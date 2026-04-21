package io.github.nicohinze.trainingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.nicohinze.trainingtracker.ui.ActiveWorkoutScreen
import io.github.nicohinze.trainingtracker.ui.WorkoutEditScreen
import io.github.nicohinze.trainingtracker.ui.WorkoutListScreen
import io.github.nicohinze.trainingtracker.ui.theme.TrainingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrainingTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "workouts") {
                        composable("workouts") {
                            WorkoutListScreen(
                                onEditWorkout = { id ->
                                    navController.navigate("edit/$id")
                                },
                                onStartWorkout = { id ->
                                    navController.navigate("active/$id")
                                },
                            )
                        }
                        composable(
                            "edit/{workoutId}",
                            arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
                        ) {
                            WorkoutEditScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            "active/{workoutId}",
                            arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
                        ) {
                            ActiveWorkoutScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
