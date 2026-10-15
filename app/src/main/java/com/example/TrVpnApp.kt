package com.example

import android.app.Application
import android.content.Context

class TrVpnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TrVpnApp
            private set

        fun getAppContext(): Context = instance.applicationContext
    }
}
