package com.example.sandbox

import android.app.Application
import org.webrtc.Logging
import timber.log.Timber
import java.util.logging.Level

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}