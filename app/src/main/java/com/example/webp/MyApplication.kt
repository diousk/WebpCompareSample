package com.example.webp

import android.app.Application
import android.util.Log
import com.facebook.common.logging.FLog
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FrescoInitializer.init(this)
//        FLog.setMinimumLoggingLevel(Log.VERBOSE)
        CoilInitializer.init(this)
        Timber.plant(Timber.DebugTree())
    }
}