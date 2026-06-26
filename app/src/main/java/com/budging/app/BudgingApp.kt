package com.budging.app

import android.app.Application
import com.budging.app.data.AppContainer

class BudgingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
