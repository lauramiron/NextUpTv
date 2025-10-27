package io.github.lauramiron.nextuptv

import android.app.Application
import io.github.lauramiron.nextuptv.data.local.DatabaseProvider

class NextUpTvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DatabaseProvider.initialize(this)
    }
}
