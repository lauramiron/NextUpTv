package io.github.lauramiron.nextuptv.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import io.github.lauramiron.nextuptv.data.ResumeRepository
import java.util.concurrent.TimeUnit

class ResumeSyncWorker(
    ctx: Context,
    params: WorkerParameters,
    private val repo: ResumeRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        repo.syncResume(limit = 100)
        return Result.success()
    }
}

// Enqueue somewhere (App.onCreate or Settings toggle)
val req = PeriodicWorkRequestBuilder<ResumeSyncWorker>(6, TimeUnit.HOURS)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
"resume-sync",
ExistingPeriodicWorkPolicy.UPDATE,
req
)
