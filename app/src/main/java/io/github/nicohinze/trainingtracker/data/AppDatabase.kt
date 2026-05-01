package io.github.nicohinze.trainingtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Workout::class, Exercise::class], version = 4)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises RENAME COLUMN reps TO amount")
                db.execSQL("ALTER TABLE exercises ADD COLUMN type TEXT NOT NULL DEFAULT 'REPS'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN intensity TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            val inst = Room
                .databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_database",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
            instance = inst
            inst
        }
    }
}
