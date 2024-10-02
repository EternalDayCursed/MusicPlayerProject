package com.example.androidmusicplayer.views

import android.content.Context
import android.util.AttributeSet
import com.example.androidmusicplayer.extension.applyColorFilter

class MySeekBar : androidx.appcompat.widget.AppCompatSeekBar {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setColors(accentColor: Int) {
        progressDrawable.applyColorFilter(accentColor)
        thumb?.applyColorFilter(accentColor)
    }
}
