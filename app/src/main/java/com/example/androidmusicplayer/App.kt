package com.example.androidmusicplayer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.androidmusicplayer.extension.checkUseEnglish
import com.example.androidmusicplayer.helper.SimpleMediaController

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
        initController()
    }

    private fun initController() {
        SimpleMediaController.getInstance(applicationContext).createControllerAsync()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    SimpleMediaController.destroyInstance()
                }
            }
        )
    }
}