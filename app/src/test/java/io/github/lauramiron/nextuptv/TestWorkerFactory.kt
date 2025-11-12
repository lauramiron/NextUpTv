package io.github.lauramiron.nextuptv

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.sync.LibrarySyncWorker

class TestWorkerFactory(
    private val repositoryProvider: () -> LibraryRepository
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (Class.forName(workerClassName)) {
            LibrarySyncWorker::class.java -> {
                LibrarySyncWorker(appContext, workerParameters, repositoryProvider())
            }
            else -> null
        }
    }
}