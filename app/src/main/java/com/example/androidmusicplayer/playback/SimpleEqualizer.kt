package com.example.androidmusicplayer.playback

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.media3.common.util.UnstableApi
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.helper.EQUALIZER_PRESET_CUSTOM
import com.example.androidmusicplayer.playback.player.SimpleMusicPlayer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@UnstableApi
object SimpleEqualizer {
    lateinit var instance: Equalizer
        internal set

    fun setupEqualizer(context: Context, player: SimpleMusicPlayer) {
        try {
            val preset = context.config.equalizerPreset
            instance = Equalizer(0, player.getAudioSessionId())
            if (!instance.enabled) {
                instance.enabled = true
            }

            if (preset != EQUALIZER_PRESET_CUSTOM) {
                instance.usePreset(preset.toShort())
            } else {
                val minValue = instance.bandLevelRange[0]
                val bandType = object : TypeToken<HashMap<Short, Int>>() {}.type
                val equalizerBands = Gson().fromJson<HashMap<Short, Int>>(context.config.equalizerBands, bandType) ?: HashMap()

                for ((key, value) in equalizerBands) {
                    val newValue = value + minValue
                    if (instance.getBandLevel(key) != newValue.toShort()) {
                        instance.setBandLevel(key, newValue.toShort())
                    }
                }
            }
        } catch (ignored: Exception) {
            context.toast(R.string.unknown_error_occurred)
        }
    }

    fun release() {
        if (SimpleEqualizer::instance.isInitialized) {
            instance.release()
        }
    }
}
