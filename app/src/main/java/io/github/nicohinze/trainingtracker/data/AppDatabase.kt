package io.github.nicohinze.trainingtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Workout::class, Exercise::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN completionCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            val inst = Room
                .databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_database",
                ).addMigrations(MIGRATION_1_2)
                .build()
            instance = inst
            inst
        }
    }
}
