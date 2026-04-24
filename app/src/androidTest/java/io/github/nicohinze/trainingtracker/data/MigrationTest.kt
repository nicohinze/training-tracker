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
        helper.createDatabase("test_db", 1).apply {
            execSQL("INSERT INTO workouts (name) VALUES ('My Workout')")
            close()
        }

        val db = helper.runMigrationsAndValidate("test_db", 2, true, AppDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT completionCount FROM workouts WHERE name = 'My Workout'")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()
        db.close()
    }
}
