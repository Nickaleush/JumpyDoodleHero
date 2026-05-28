package com.businessdoomguy.minigamestarter

import android.app.Application
import com.businessdoomguy.minigamestarter.core.AppServiceLocator

class App : Application() {

    lateinit var serviceLocator: AppServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = AppServiceLocator(this)
    }

    override fun onTerminate() {
        // Note: onTerminate is only called on emulators, but releasing audio
        // resources here is still correct and harmless on real devices where
        // the OS reclaims the process directly.
        serviceLocator.soundManager.release()
        super.onTerminate()
    }
}
