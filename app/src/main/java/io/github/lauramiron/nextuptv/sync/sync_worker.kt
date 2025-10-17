package io.github.lauramiron.nextuptv.sync

import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class MovieNightSyncWorker(
    ctx: Context,
    params: WorkerParameters,
    private val repo: MovieNightRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // strategy: pull recent/changed titles; or sync everything on first run
        val targets = inputData.getStringArray(KEY_MON_IDS)?.toList().orEmpty()
        targets.forEach { monId ->
            try { repo.syncTitle(monId) } catch (t: Throwable) {
                // log and continue; or return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_MON_IDS = "mon_ids"

        fun schedule(context: Context, monIds: List<String>) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val data = workDataOf(KEY_MON_IDS to monIds.toTypedArray())

            val request = PeriodicWorkRequestBuilder<MovieNightSyncWorker>(12, java.util.concurrent.TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "movieNightPeriodicSync",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
        }
    }
}
