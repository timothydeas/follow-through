package com.example.grounded

import android.app.Application
import com.example.grounded.di.AppContainer

class GroundedApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
