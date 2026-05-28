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
        serviceLocator.soundManager.release()
        super.onTerminate()
    }
}
