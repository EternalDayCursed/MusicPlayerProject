package com.example.androidmusicplayer.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.databinding.MenuSearchBinding
import com.example.androidmusicplayer.extension.adjustAlpha
import com.example.androidmusicplayer.extension.applyColorFilter
import com.example.androidmusicplayer.extension.getContrastColor
import com.example.androidmusicplayer.extension.getProperBackgroundColor
import com.example.androidmusicplayer.extension.getProperPrimaryColor
import com.example.androidmusicplayer.extension.hideKeyboard
import com.example.androidmusicplayer.extension.onTextChangeListener
import com.example.androidmusicplayer.extension.removeBit
import com.example.androidmusicplayer.extension.showKeyboard
import com.example.androidmusicplayer.helper.LOWER_ALPHA
import com.example.androidmusicplayer.helper.MEDIUM_ALPHA
import com.google.android.material.appbar.AppBarLayout

class MySearchMenu(context: Context, attrs: AttributeSet) : AppBarLayout(context, attrs) {
    var isSearchOpen = false
    var useArrowIcon = false
    var onSearchOpenListener: (() -> Unit)? = null
    var onSearchClosedListener: (() -> Unit)? = null
    var onSearchTextChangedListener: ((text: String) -> Unit)? = null
    var onNavigateBackClickListener: (() -> Unit)? = null

    val binding = MenuSearchBinding.inflate(LayoutInflater.from(context), this, true)

    fun getToolbar() = binding.topToolbar

    fun setupMenu() {
        binding.topToolbarSearchIcon.setOnClickListener {
            if (isSearchOpen) {
                closeSearch()
            } else if (useArrowIcon && onNavigateBackClickListener != null) {
                onNavigateBackClickListener!!()
            } else {
                binding.topToolbarSearch.requestFocus()
                (context as? Activity)?.showKeyboard(binding.topToolbarSearch)
            }
        }

        post {
            binding.topToolbarSearch.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    openSearch()
                }
            }
        }

        binding.topToolbarSearch.onTextChangeListener { text ->
            onSearchTextChangedListener?.invoke(text)
        }
    }

    private fun openSearch() {
        isSearchOpen = true
        onSearchOpenListener?.invoke()
        binding.topToolbarSearchIcon.setImageResource(R.drawable.ic_arrow_left_vector)
        binding.topToolbarSearchIcon.contentDescription = resources.getString(R.string.back)
    }

    fun closeSearch() {
        isSearchOpen = false
        onSearchClosedListener?.invoke()
        binding.topToolbarSearch.setText("")
        if (!useArrowIcon) {
            binding.topToolbarSearchIcon.setImageResource(R.drawable.ic_search_vector)
            binding.topToolbarSearchIcon.contentDescription = resources.getString(R.string.search)
        }
        (context as? Activity)?.hideKeyboard()
    }

    fun toggleHideOnScroll(hideOnScroll: Boolean) {
        val params = binding.topAppBarLayout.layoutParams as LayoutParams
        if (hideOnScroll) {
            params.scrollFlags = LayoutParams.SCROLL_FLAG_SCROLL or LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else {
            params.scrollFlags = params.scrollFlags.removeBit(LayoutParams.SCROLL_FLAG_SCROLL or LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
        }
    }

    fun updateColors() {
        val backgroundColor = context.getProperBackgroundColor()
        val contrastColor = backgroundColor.getContrastColor()

        setBackgroundColor(backgroundColor)
        binding.topAppBarLayout.setBackgroundColor(backgroundColor)
        binding.topToolbarSearchIcon.applyColorFilter(contrastColor)
        binding.topToolbarHolder.background?.applyColorFilter(context.getProperPrimaryColor().adjustAlpha(LOWER_ALPHA))
        binding.topToolbarSearch.setTextColor(contrastColor)
        binding.topToolbarSearch.setHintTextColor(contrastColor.adjustAlpha(MEDIUM_ALPHA))
        (context as? BaseCustomActivity)?.updateTopBarColors(binding.topToolbar, backgroundColor)
    }
}
