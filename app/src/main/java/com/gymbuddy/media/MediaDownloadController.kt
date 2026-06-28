package com.gymbuddy.media

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Drives the bulk media download and exposes its progress to the UI. */
class MediaDownloadController(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    val state: Flow<DownloadProgress> =
        workManager.getWorkInfosForUniqueWorkFlow(MediaDownloadWorker.WORK_NAME).map { infos ->
            val info = infos.firstOrNull() ?: return@map DownloadProgress.Idle
            when (info.state) {
                WorkInfo.State.RUNNING -> {
                    val current = info.progress.getInt(MediaDownloadWorker.KEY_CURRENT, 0)
                    val total = info.progress.getInt(MediaDownloadWorker.KEY_TOTAL, 0)
                    DownloadProgress.Running(current, total)
                }
                WorkInfo.State.ENQUEUED -> DownloadProgress.Running(0, 0)
                WorkInfo.State.SUCCEEDED -> DownloadProgress.Done
                WorkInfo.State.FAILED -> DownloadProgress.Failed
                WorkInfo.State.CANCELLED -> DownloadProgress.Idle
                WorkInfo.State.BLOCKED -> DownloadProgress.Running(0, 0)
            }
        }

    fun start() {
        val request = OneTimeWorkRequestBuilder<MediaDownloadWorker>().build()
        workManager.enqueueUniqueWork(
            MediaDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(MediaDownloadWorker.WORK_NAME)
    }
}

sealed interface DownloadProgress {
    data object Idle : DownloadProgress
    data class Running(val current: Int, val total: Int) : DownloadProgress
    data object Done : DownloadProgress
    data object Failed : DownloadProgress
}
