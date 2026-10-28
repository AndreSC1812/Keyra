package com.ascdev.keyra

import android.app.Application
import com.ascdev.keyra.core.session.AutoLockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KeyraApp : Application() {

    @Inject lateinit var autoLockManager: AutoLockManager

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        autoLockManager.start()
    }
}
