package io.github.nicohinze.trainingtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutJsonConverterTest {
    @Test
    fun toJson_emptyList_producesEmptyWorkoutsArray() {
        val json = WorkoutJsonConverter.toJson(emptyList())
        val parsed = WorkoutJsonConverter.fromJson(json)
        assertEquals(0, parsed.size)
    }

    @Test
    fun roundtrip_singleWorkoutNoExercises() {
        val workout = Workout(name = "Rest Day", completionCount = 2, totalDurationSeconds = 600)
        val input = listOf(Pair(workout, emptyList<Exercise>()))

        val json = WorkoutJsonConverter.toJson(input)
        val result = WorkoutJsonConverter.fromJson(json)

        assertEquals(1, result.size)
        val (parsedWorkout, parsedExercises) = result[0]
        assertEquals("Rest Day", parsedWorkout.name)
        assertEquals(2, parsedWorkout.completionCount)
        assertEquals(600L, parsedWorkout.totalDurationSeconds)
        assertEquals(0, parsedExercises.size)
    }

    @Test
    fun roundtrip_workoutWithExercises() {
        val workout = Workout(name = "Push Day", completionCount = 5, totalDurationSeconds = 3600)
        val exercises = listOf(
            Exercise(
                workoutId = 1,
                name = "Bench Press",
                sets = 3,
                amount = 10,
                type = ExerciseType.REPS,
                intensity = "80kg",
                pauseSeconds = 90,
                orderIndex = 0,
            ),
            Exercise(
                workoutId = 1,
                name = "Plank",
                sets = 2,
                amount = 60,
                type = ExerciseType.SECONDS,
                intensity = null,
                pauseSeconds = 30,
                orderIndex = 1,
            ),
        )
        val input = listOf(Pair(workout, exercises))

        val json = WorkoutJsonConverter.toJson(input)
        val result = WorkoutJsonConverter.fromJson(json)

        assertEquals(1, result.size)
        val (_, parsedExercises) = result[0]
        assertEquals(2, parsedExercises.size)

        val first = parsedExercises[0]
        assertEquals("Bench Press", first.name)
        assertEquals(3, first.sets)
        assertEquals(10, first.amount)
        assertEquals(ExerciseType.REPS, first.type)
        assertEquals("80kg", first.intensity)
        assertEquals(90, first.pauseSeconds)
        assertEquals(0, first.orderIndex)

        val second = parsedExercises[1]
        assertEquals("Plank", second.name)
        assertEquals(2, second.sets)
        assertEquals(60, second.amount)
        assertEquals(ExerciseType.SECONDS, second.type)
        assertNull(second.intensity)
        assertEquals(30, second.pauseSeconds)
        assertEquals(1, second.orderIndex)
    }

    @Test
    fun roundtrip_multipleWorkouts() {
        val input = listOf(
            Pair(
                Workout(name = "Push", completionCount = 1, totalDurationSeconds = 1800),
                listOf(
                    Exercise(
                        workoutId = 1,
                        name = "Press",
                        sets = 3,
                        amount = 8,
                        pauseSeconds = 60,
                        orderIndex = 0,
                    ),
                ),
            ),
            Pair(
                Workout(name = "Pull", completionCount = 3, totalDurationSeconds = 2400),
                listOf(
                    Exercise(
                        workoutId = 2,
                        name = "Row",
                        sets = 4,
                        amount = 12,
                        pauseSeconds = 90,
                        orderIndex = 0,
                    ),
                    Exercise(
                        workoutId = 2,
                        name = "Curl",
                        sets = 3,
                        amount = 10,
                        intensity = "12kg",
                        pauseSeconds = 45,
                        orderIndex = 1,
                    ),
                ),
            ),
        )

        val json = WorkoutJsonConverter.toJson(input)
        val result = WorkoutJsonConverter.fromJson(json)

        assertEquals(2, result.size)
        assertEquals("Push", result[0].first.name)
        assertEquals(1, result[0].second.size)
        assertEquals("Pull", result[1].first.name)
        assertEquals(2, result[1].second.size)
    }

    @Test
    fun fromJson_missingOptionalFields_usesDefaults() {
        val json =
            """
            {
              "workouts": [
                {
                  "name": "Minimal",
                  "exercises": [
                    {
                      "name": "Pushups",
                      "sets": 3,
                      "amount": 20,
                      "pauseSeconds": 60,
                      "orderIndex": 0
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val result = WorkoutJsonConverter.fromJson(json)

        assertEquals(1, result.size)
        val (workout, exercises) = result[0]
        assertEquals("Minimal", workout.name)
        assertEquals(0, workout.completionCount)
        assertEquals(0L, workout.totalDurationSeconds)
        assertEquals(1, exercises.size)
        assertEquals(ExerciseType.REPS, exercises[0].type)
        assertNull(exercises[0].intensity)
    }

    @Test
    fun fromJson_noExercisesKey_treatsAsEmpty() {
        val json =
            """
            {
              "workouts": [
                {
                  "name": "Empty Workout",
                  "completionCount": 0,
                  "totalDurationSeconds": 0
                }
              ]
            }
            """.trimIndent()

        val result = WorkoutJsonConverter.fromJson(json)

        assertEquals(1, result.size)
        assertEquals(0, result[0].second.size)
    }

    @Test(expected = org.json.JSONException::class)
    fun fromJson_invalidJson_throws() {
        WorkoutJsonConverter.fromJson("not json")
    }

    @Test(expected = org.json.JSONException::class)
    fun fromJson_missingWorkoutsKey_throws() {
        WorkoutJsonConverter.fromJson("""{"data": []}""")
    }

    @Test
    fun fromJson_parsedExercisesHaveZeroWorkoutId() {
        val json =
            """
            {
              "workouts": [
                {
                  "name": "Test",
                  "exercises": [
                    {
                      "name": "Ex",
                      "sets": 1,
                      "amount": 1,
                      "pauseSeconds": 30,
                      "orderIndex": 0
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val result = WorkoutJsonConverter.fromJson(json)
        assertEquals(0L, result[0].second[0].workoutId)
    }

    @Test
    fun roundtrip_preservesColor() {
        val color = 0xFF4CAF50.toInt()
        val workout = Workout(name = "Green", color = color)
        val input = listOf(Pair(workout, emptyList<Exercise>()))

        val json = WorkoutJsonConverter.toJson(input)
        val result = WorkoutJsonConverter.fromJson(json)

        assertEquals(color, result[0].first.color)
    }

    @Test
    fun fromJson_missingColor_usesDefault() {
        val json =
            """
            {
              "workouts": [
                {
                  "name": "No Color",
                  "completionCount": 0,
                  "totalDurationSeconds": 0,
                  "exercises": []
                }
              ]
            }
            """.trimIndent()

        val result = WorkoutJsonConverter.fromJson(json)
        assertEquals(DEFAULT_WORKOUT_COLOR, result[0].first.color)
    }

    @Test
    fun roundtrip_multipleWorkoutsPreserveDifferentColors() {
        val input = listOf(
            Pair(Workout(name = "Blue", color = 0xFF2196F3.toInt()), emptyList<Exercise>()),
            Pair(Workout(name = "Red", color = 0xFFF44336.toInt()), emptyList<Exercise>()),
        )

        val json = WorkoutJsonConverter.toJson(input)
        val result = WorkoutJsonConverter.fromJson(json)

        assertEquals(0xFF2196F3.toInt(), result[0].first.color)
        assertEquals(0xFFF44336.toInt(), result[1].first.color)
    }
}
