package com.freshsave.app

import android.app.Application
import com.freshsave.app.di.AppContainer

/** Android creates this once per app launch, before any screen. It owns the [AppContainer]. */
class FreshSaveApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
