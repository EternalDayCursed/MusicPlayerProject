package com.example.androidmusicplayer.helper

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.androidmusicplayer.extension.getOrNull
import com.example.androidmusicplayer.extension.runOnPlayerThread
import com.example.androidmusicplayer.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors

class SimpleMediaController(val context: Application) {
    private val executorService by lazy {
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    }

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    @Synchronized
    fun createControllerAsync() {
        controllerFuture = MediaController
            .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()

        controllerFuture.addListener({
            controller = getControllerSync()
        }, MoreExecutors.directExecutor())
    }

    private fun getControllerSync() = controllerFuture.getOrNull()

    private fun shouldCreateNewController(): Boolean {
        return if (!::controllerFuture.isInitialized) {
            return true
        } else {
            controllerFuture.isCancelled || controllerFuture.isDone && getControllerSync()?.isConnected == false
        }
    }

    private fun acquireController(callback: (() -> Unit)? = null) {
        executorService.execute {
            if (shouldCreateNewController()) {
                createControllerAsync()
            } else {
                controller = getControllerSync()
            }

            callback?.invoke()
        }
    }

    fun releaseController() {
        MediaController.releaseFuture(controllerFuture)
    }

    fun withController(callback: MediaController.() -> Unit) {
        val controller = controller
        if (controller != null && controller.isConnected) {
            controller.runOnPlayerThread(callback)
        } else {
            acquireController {
                getControllerSync()?.runOnPlayerThread(callback)
            }
        }
    }

    fun addListener(listener: Player.Listener) {
        withController {
            addListener(listener)
        }
    }

    fun removeListener(listener: Player.Listener) {
        withController {
            removeListener(listener)
        }
    }

    companion object {
        private var instance: SimpleMediaController? = null

        fun getInstance(context: Context): SimpleMediaController {
            if (instance == null) {
                instance = SimpleMediaController(context.applicationContext as Application)
            }

            return instance!!
        }

        fun destroyInstance() {
            instance?.releaseController()
            instance = null
        }
    }
}
