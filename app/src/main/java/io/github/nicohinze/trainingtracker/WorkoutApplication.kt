package io.github.nicohinze.trainingtracker

import android.app.Application
import io.github.nicohinze.trainingtracker.data.AppDatabase

class WorkoutApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
