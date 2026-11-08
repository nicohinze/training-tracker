package io.github.nicohinze.trainingtracker

import kotlinx.serialization.Serializable

@Serializable
object WorkoutListRoute

@Serializable
object AboutRoute

@Serializable
object ActivityGraphRoute

@Serializable
data class WorkoutEditRoute(
    val workoutId: Long,
)

@Serializable
data class ActiveWorkoutRoute(
    val workoutId: Long,
)
