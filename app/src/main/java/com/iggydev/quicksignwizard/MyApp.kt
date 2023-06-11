package com.iggydev.quicksignwizard

import android.app.Application
import android.os.Bundle
import com.iggydev.quicksignwizard.di.appModule
import org.koin.core.context.GlobalContext.startKoin

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(appModule)
        }
    }
}