package io.github.lauramiron.nextuptv.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.lauramiron.nextuptv.data.LibraryRepository

enum class SyncMode { FULL, INCREMENTAL, SINGLE_TITLE }
class MovieNightSyncWorker(
    ctx: Context,
    params: WorkerParameters,
    private val repo: LibraryRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val mode = inputData.getString("mode")?.let { SyncMode.valueOf(it) } ?: SyncMode.FULL
        return when (mode) {
            SyncMode.FULL -> runFullSync()
            SyncMode.INCREMENTAL -> runFullSync()
            SyncMode.SINGLE_TITLE -> runFullSync()
//            SyncMode.INCREMENTAL -> runIncrementalSync(
//                since = inputData.getLong("since_epoch_ms", 0L)
//            )
//            SyncMode.SINGLE_TITLE -> syncSingleTitle(
//                monId = inputData.getString("mon_id") ?: return Result.failure()
//            )
        }
    }

    private suspend fun runFullSync(): Result {
        repo.syncAll()
        return Result.success()
    }

//    private suspend fun runIncrementalSync(since: Long): Result {
//        repo.syncUpdatedSince(since)
//        return Result.success()
//    }

//    private suspend fun syncSingleTitle(monId: String): Result {
//        repo.syncTitle(monId)
//        return Result.success()
//    }

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
