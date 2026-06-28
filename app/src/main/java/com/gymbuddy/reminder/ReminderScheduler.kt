package com.gymbuddy.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gymbuddy.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Schedules a daily "time to train" reminder notification. */
object ReminderScheduler {
    private const val WORK = "daily_reminder"
    const val CHANNEL = "reminders"

    fun schedule(context: Context, hour: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
}

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    ReminderScheduler.CHANNEL,
                    "Workout reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL)
            .setContentTitle("Time to train 💪")
            .setContentText("Your next workout is waiting. Let's keep the streak alive!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        runCatching { mgr.notify(9911, notification) }
        return Result.success()
    }
}
