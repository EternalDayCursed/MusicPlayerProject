package com.example.androidmusicplayer.extension

import android.os.Handler
import android.os.Looper

fun Looper.post(callback: () -> Unit) = Handler(this).post(callback)