package com.example.androidmusicplayer.extension

import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import androidx.viewbinding.ViewBinding
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.helper.SHORT_ANIMATION_DURATION

inline fun <T : ViewBinding> View.viewBinding(crossinline bind: (View) -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        bind(this)
    }

fun View.beInvisibleIf(beInvisible: Boolean) = if (beInvisible) beInvisible() else beVisible()
fun View.beVisibleIf(beVisible: Boolean) = if (beVisible) beVisible() else beGone()
fun View.beGoneIf(beGone: Boolean) = beVisibleIf(!beGone)

fun View.beInvisible() {
    visibility = View.INVISIBLE
}

fun View.beVisible() {
    visibility = View.VISIBLE
}

fun View.beGone() {
    visibility = View.GONE
}

fun View.onGlobalLayout(callback: () -> Unit) {
    viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (viewTreeObserver != null) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                callback()
            }
        }
    })
}

fun View.isVisible() = visibility == View.VISIBLE

fun View.fadeIn() {
    animate().alpha(1f).setDuration(SHORT_ANIMATION_DURATION).withStartAction { beVisible() }.start()
}

fun View.fadeOut() {
    animate().alpha(0f).setDuration(SHORT_ANIMATION_DURATION).withEndAction { beGone() }.start()
}

fun View.setupViewBackground(context: Context) {
    background = if (context.baseConfig.isUsingSystemTheme) {
        resources.getDrawable(R.drawable.selector_clickable_you)
    } else {
        resources.getDrawable(R.drawable.selector_clickable)
    }
}