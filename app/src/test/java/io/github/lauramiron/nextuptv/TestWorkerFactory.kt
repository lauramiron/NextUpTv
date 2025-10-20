package io.github.lauramiron.nextuptva

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
        params: WorkerParameters
    ): ListenableWorker? {
        return when (Class.forName(workerClassName)) {
            LibrarySyncWorker::class.java -> {
                LibrarySyncWorker(appContext, params, repositoryProvider())
            }
            else -> null
        }
    }
}