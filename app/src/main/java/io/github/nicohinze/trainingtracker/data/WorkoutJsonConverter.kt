package io.github.nicohinze.trainingtracker.data

import org.json.JSONArray
import org.json.JSONObject

object WorkoutJsonConverter {
    fun toJson(workoutsWithExercises: List<Pair<Workout, List<Exercise>>>): String {
        val root = JSONObject()
        val workoutsArray = JSONArray()
        for ((workout, exercises) in workoutsWithExercises) {
            workoutsArray.put(workoutToJson(workout, exercises))
        }
        root.put("workouts", workoutsArray)
        return root.toString(2)
    }

    fun fromJson(json: String): List<Pair<Workout, List<Exercise>>> {
        val root = JSONObject(json)
        val workoutsArray = root.getJSONArray("workouts")
        val result = mutableListOf<Pair<Workout, List<Exercise>>>()
        for (i in 0 until workoutsArray.length()) {
            val workoutObj = workoutsArray.getJSONObject(i)
            val workout = Workout(
                name = workoutObj.getString("name"),
                completionCount = workoutObj.optInt("completionCount", 0),
                totalDurationSeconds = workoutObj.optLong("totalDurationSeconds", 0),
                color = workoutObj.optInt("color", DEFAULT_WORKOUT_COLOR),
            )
            val exercisesArray = workoutObj.optJSONArray("exercises") ?: JSONArray()
            val exercises = mutableListOf<Exercise>()
            for (j in 0 until exercisesArray.length()) {
                val exerciseObj = exercisesArray.getJSONObject(j)
                exercises.add(
                    Exercise(
                        workoutId = 0,
                        name = exerciseObj.getString("name"),
                        sets = exerciseObj.getInt("sets"),
                        amount = exerciseObj.getInt("amount"),
                        type = ExerciseType.valueOf(exerciseObj.optString("type", "REPS")),
                        intensity = exerciseObj.optString("intensity").takeIf { it != "null" && it.isNotEmpty() },
                        pauseSeconds = exerciseObj.getInt("pauseSeconds"),
                        orderIndex = exerciseObj.getInt("orderIndex"),
                    ),
                )
            }
            result.add(Pair(workout, exercises))
        }
        return result
    }

    private fun workoutToJson(workout: Workout, exercises: List<Exercise>): JSONObject {
        val obj = JSONObject()
        obj.put("name", workout.name)
        obj.put("completionCount", workout.completionCount)
        obj.put("totalDurationSeconds", workout.totalDurationSeconds)
        obj.put("color", workout.color)
        val exercisesArray = JSONArray()
        for (exercise in exercises) {
            exercisesArray.put(exerciseToJson(exercise))
        }
        obj.put("exercises", exercisesArray)
        return obj
    }

    private fun exerciseToJson(exercise: Exercise): JSONObject {
        val obj = JSONObject()
        obj.put("name", exercise.name)
        obj.put("sets", exercise.sets)
        obj.put("amount", exercise.amount)
        obj.put("type", exercise.type.name)
        obj.put("intensity", exercise.intensity ?: JSONObject.NULL)
        obj.put("pauseSeconds", exercise.pauseSeconds)
        obj.put("orderIndex", exercise.orderIndex)
        return obj
    }
}
