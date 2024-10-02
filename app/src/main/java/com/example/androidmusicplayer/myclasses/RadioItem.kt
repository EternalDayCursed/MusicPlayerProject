package com.example.androidmusicplayer.myclasses

import androidx.compose.runtime.Immutable

@Immutable
data class RadioItem(val id: Int, val title: String, val value: Any = id)