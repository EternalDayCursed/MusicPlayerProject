package com.example.androidmusicplayer.fragments

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.example.androidmusicplayer.activity.CustomActivity
import com.example.androidmusicplayer.activity.CustomControllerActivity
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.models.Track

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    abstract fun setupFragment(activity: BaseCustomActivity)

    abstract fun finishActMode()

    abstract fun onSearchQueryChanged(text: String)

    abstract fun onSearchClosed()

    abstract fun onSortOpen(activity: CustomActivity)

    abstract fun setupColors(textColor: Int, adjustedPrimaryColor: Int)

    fun prepareAndPlay(tracks: List<Track>, startIndex: Int = 0, startPositionMs: Long = 0, startActivity: Boolean = true) {
        (context as CustomControllerActivity).prepareAndPlay(tracks, startIndex, startPositionMs, startActivity)
    }
}