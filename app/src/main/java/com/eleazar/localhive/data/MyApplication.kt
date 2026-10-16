package com.eleazar.localhive.data

import android.app.Application
import com.eleazar.localhive.BuildConfig
import com.eleazar.localhive.di.initKoin
import org.koin.android.ext.koin.androidContext

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Before anything talks to Firebase, so every request carries an App Check token.
        installAppCheck()
        initKoin(apiBaseUrl = BuildConfig.LOCALHIVE_API_URL) {
            androidContext(this@MyApplication)
        }
    }
}
