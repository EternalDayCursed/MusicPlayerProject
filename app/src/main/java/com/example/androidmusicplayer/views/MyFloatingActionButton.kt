package com.example.androidmusicplayer.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.example.androidmusicplayer.extension.applyColorFilter
import com.example.androidmusicplayer.extension.getContrastColor
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MyFloatingActionButton : FloatingActionButton {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setColors(accentColor: Int) {
        backgroundTintList = ColorStateList.valueOf(accentColor)
        applyColorFilter(accentColor.getContrastColor())
    }
}
