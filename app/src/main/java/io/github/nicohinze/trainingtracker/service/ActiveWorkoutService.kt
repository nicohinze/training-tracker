package io.github.nicohinze.trainingtracker.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.github.nicohinze.trainingtracker.R
import io.github.nicohinze.trainingtracker.viewmodel.ActiveState
import io.github.nicohinze.trainingtracker.viewmodel.ActiveWorkoutUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "active_workout"
private const val NOTIFICATION_ID = 1
private const val MS_PER_SECOND = 1000L
private const val WAKE_LOCK_MARGIN_MS = 5000L

/**
 * Keeps the process alive while a workout is in progress, so the rest timer in ActiveWorkoutViewModel keeps
 * running and vibrating while the app is in the background, and shows the session in an ongoing notification.
 */
class ActiveWorkoutService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val wakeLock by lazy {
        getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TrainingTracker:RestTimer")
            .apply { setReferenceCounted(false) }
    }
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Active workout", NotificationManager.IMPORTANCE_LOW),
        )
        scope.launch {
            // Only re-post when the phase changes; the chronometer takes care of the per-second updates.
            session
                .distinctUntilChangedBy { it?.let { s -> Triple(s.state, s.currentExerciseIndex, s.completedSets) } }
                .collect { state ->
                    if (state == null) {
                        stop()
                    } else {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(state))
                        updateWakeLock(state)
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        // Must always be called after startForegroundService(), even if the workout has already ended.
        val state = session.value
        startForeground(
            NOTIFICATION_ID,
            if (state != null) notification(state) else emptyNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        if (state == null) {
            stop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock.release()
        super.onDestroy()
    }

    private fun stop() {
        wakeLock.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(lastStartId)
    }

    // The CPU may sleep while the screen is off, which would delay the end-of-rest vibration.
    private fun updateWakeLock(state: ActiveWorkoutUiState) {
        if (state.state == ActiveState.RESTING) {
            wakeLock.acquire(state.remainingSeconds * MS_PER_SECOND + WAKE_LOCK_MARGIN_MS)
        } else {
            wakeLock.release()
        }
    }

    private fun notification(state: ActiveWorkoutUiState): Notification {
        val content = workoutNotificationContent(state, System.currentTimeMillis())
        return baseNotification()
            .setContentTitle(content.title)
            .setContentText(content.text)
            .apply {
                content.chronometer?.let {
                    setWhen(it.whenMs)
                    setShowWhen(true)
                    setUsesChronometer(true)
                    setChronometerCountDown(it.countDown)
                }
            }.build()
    }

    private fun emptyNotification(): Notification = baseNotification().setContentTitle("Workout").build()

    private fun baseNotification(): NotificationCompat.Builder {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }

    companion object {
        private val session = MutableStateFlow<ActiveWorkoutUiState?>(null)

        /** Starts, updates, or stops the service for [state]; null means there is no workout anymore. */
        fun update(context: Context, state: ActiveWorkoutUiState?) {
            val wasInProgress = session.value != null
            val inProgress = state?.takeIf { it.isInProgress }
            session.value = inProgress
            if (inProgress != null && !wasInProgress) {
                try {
                    context.startForegroundService(Intent(context, ActiveWorkoutService::class.java))
                } catch (_: ForegroundServiceStartNotAllowedException) {
                    // Only possible if the workout was started from the background; it then simply
                    // runs without the service, as before.
                }
            }
        }
    }
}
