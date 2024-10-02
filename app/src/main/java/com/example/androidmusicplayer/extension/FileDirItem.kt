package com.example.androidmusicplayer.extension

import android.content.Context
import com.example.androidmusicplayer.myclasses.FileDirItem

fun FileDirItem.isRecycleBinPath(context: Context): Boolean {
    return path.startsWith(context.recycleBinPath)
}
