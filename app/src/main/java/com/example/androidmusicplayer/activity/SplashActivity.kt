package com.example.androidmusicplayer.activity

import android.annotation.SuppressLint
import android.content.Intent
import com.example.androidmusicplayer.activity.base.BaseSplashActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}