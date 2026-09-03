package io.github.nicohinze.trainingtracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsCompletionCountColumn() {
        helper.createDatabase("test_db_1_2", 1).apply {
            execSQL("INSERT INTO workouts (name) VALUES ('My Workout')")
            close()
        }

        val db = helper.runMigrationsAndValidate("test_db_1_2", 2, true, AppDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT completionCount FROM workouts WHERE name = 'My Workout'")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migrate2To3_renamesRepsToAmountAndAddsTypeColumn() {
        helper.createDatabase("test_db_2_3", 2).apply {
            execSQL("INSERT INTO workouts (name, completionCount) VALUES ('Test', 0)")
            execSQL(
                "INSERT INTO exercises (workoutId, name, sets, reps, pauseSeconds, orderIndex) " +
                    "VALUES (1, 'Pushups', 3, 10, 60, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("test_db_2_3", 3, true, AppDatabase.MIGRATION_2_3)

        val cursor = db.query("SELECT amount, type FROM exercises WHERE name = 'Pushups'")
        cursor.moveToFirst()
        assertEquals(10, cursor.getInt(0))
        assertEquals("REPS", cursor.getString(1))
        cursor.close()
        db.close()
    }

    @Test
    fun migrate3To4_addsIntensityColumn() {
        helper.createDatabase("test_db_3_4", 3).apply {
            execSQL("INSERT INTO workouts (name, completionCount) VALUES ('Test', 0)")
            execSQL(
                "INSERT INTO exercises (workoutId, name, sets, amount, type, pauseSeconds, orderIndex) " +
                    "VALUES (1, 'Bench', 3, 10, 'REPS', 90, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("test_db_3_4", 4, true, AppDatabase.MIGRATION_3_4)

        val cursor = db.query("SELECT intensity FROM exercises WHERE name = 'Bench'")
        cursor.moveToFirst()
        assertEquals(true, cursor.isNull(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migrate4To5_addsTotalDurationSecondsColumn() {
        helper.createDatabase("test_db_4_5", 4).apply {
            execSQL("INSERT INTO workouts (name, completionCount) VALUES ('Test', 5)")
            close()
        }

        val db = helper.runMigrationsAndValidate("test_db_4_5", 5, true, AppDatabase.MIGRATION_4_5)

        val cursor = db.query("SELECT totalDurationSeconds FROM workouts WHERE name = 'Test'")
        cursor.moveToFirst()
        assertEquals(0, cursor.getLong(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migrate5To6_addsColorColumnAndCompletionsTable() {
        helper.createDatabase("test_db_5_6", 5).apply {
            execSQL(
                "INSERT INTO workouts (name, completionCount, totalDurationSeconds) VALUES ('Test', 3, 900)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("test_db_5_6", 6, true, AppDatabase.MIGRATION_5_6)

        val workoutCursor = db.query("SELECT color FROM workouts WHERE name = 'Test'")
        workoutCursor.moveToFirst()
        assertEquals(0xFF2196F3.toInt(), workoutCursor.getInt(0))
        workoutCursor.close()

        db.execSQL(
            "INSERT INTO workout_completions (workoutId, completedAt, durationSeconds) VALUES (1, 1000000, 300)",
        )
        val completionCursor = db.query("SELECT workoutId, completedAt, durationSeconds FROM workout_completions")
        completionCursor.moveToFirst()
        assertEquals(1, completionCursor.getLong(0))
        assertEquals(1000000L, completionCursor.getLong(1))
        assertEquals(300L, completionCursor.getLong(2))
        completionCursor.close()

        db.close()
    }

    @Test
    fun migrate5To6_completionsTableHasIndexes() {
        helper.createDatabase("test_db_5_6_idx", 5).apply {
            close()
        }

        val db = helper.runMigrationsAndValidate("test_db_5_6_idx", 6, true, AppDatabase.MIGRATION_5_6)

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'workout_completions'",
        )
        val indexNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indexNames.add(cursor.getString(0))
        }
        cursor.close()

        assertEquals(true, indexNames.any { it.contains("workoutId") })
        assertEquals(true, indexNames.any { it.contains("completedAt") })

        db.close()
    }

    @Test
    fun migrateAll() {
        helper.createDatabase("test_db_all", 1).apply {
            execSQL("INSERT INTO workouts (name) VALUES ('Full Migration')")
            execSQL(
                "INSERT INTO exercises (workoutId, name, sets, reps, pauseSeconds, orderIndex) " +
                    "VALUES (1, 'Pushups', 3, 15, 60, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test_db_all",
            6,
            true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
        )

        val workoutCursor = db.query(
            "SELECT name, completionCount, totalDurationSeconds, color FROM workouts",
        )
        workoutCursor.moveToFirst()
        assertEquals("Full Migration", workoutCursor.getString(0))
        assertEquals(0, workoutCursor.getInt(1))
        assertEquals(0L, workoutCursor.getLong(2))
        assertEquals(0xFF2196F3.toInt(), workoutCursor.getInt(3))
        workoutCursor.close()

        val exerciseCursor = db.query(
            "SELECT name, sets, amount, type, intensity, pauseSeconds, orderIndex FROM exercises",
        )
        exerciseCursor.moveToFirst()
        assertEquals("Pushups", exerciseCursor.getString(0))
        assertEquals(3, exerciseCursor.getInt(1))
        assertEquals(15, exerciseCursor.getInt(2))
        assertEquals("REPS", exerciseCursor.getString(3))
        assertEquals(true, exerciseCursor.isNull(4))
        assertEquals(60, exerciseCursor.getInt(5))
        assertEquals(0, exerciseCursor.getInt(6))
        exerciseCursor.close()

        db.close()
    }
}
