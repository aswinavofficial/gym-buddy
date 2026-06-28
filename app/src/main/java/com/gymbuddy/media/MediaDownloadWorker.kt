package com.gymbuddy.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import coil.request.ImageRequest
import com.gymbuddy.GymBuddyApp
import com.gymbuddy.R

/** Pre-fetches every exercise image + GIF into the Coil disk cache for full offline use. */
class MediaDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GymBuddyApp
        val container = app.container
        val loader = container.imageLoader

        val paths = container.database.exerciseDao().allMediaPaths()
        val urls = buildList {
            paths.forEach { p ->
                if (p.image.isNotBlank()) add(mediaUrl(p.image))
                if (p.gifUrl.isNotBlank()) add(mediaUrl(p.gifUrl))
            }
        }
        val total = urls.size
        if (total == 0) return Result.success()

        setForeground(foregroundInfo(0, total))

        var done = 0
        for (url in urls) {
            if (isStopped) return Result.success()
            val request = ImageRequest.Builder(applicationContext)
                .data(url)
                .build()
            runCatching { loader.execute(request) }
            done++
            if (done % 5 == 0 || done == total) {
                setProgress(workDataOf(KEY_CURRENT to done, KEY_TOTAL to total))
                setForeground(foregroundInfo(done, total))
            }
        }
        setProgress(workDataOf(KEY_CURRENT to total, KEY_TOTAL to total))
        return Result.success()
    }

    private fun foregroundInfo(current: Int, total: Int): ForegroundInfo {
        val ctx = applicationContext
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL, "Offline download", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification = NotificationCompat.Builder(ctx, CHANNEL)
            .setContentTitle("Downloading exercise media")
            .setContentText("$current / $total")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setProgress(total, current, false)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_ID, notification)
        }
    }

    companion object {
        const val WORK_NAME = "media_download"
        const val KEY_CURRENT = "current"
        const val KEY_TOTAL = "total"
        private const val CHANNEL = "offline_download"
        private const val NOTIF_ID = 4711
    }
}
