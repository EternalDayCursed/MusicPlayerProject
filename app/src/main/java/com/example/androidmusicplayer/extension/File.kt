package com.example.androidmusicplayer.extension

import android.content.Context
import com.example.androidmusicplayer.helper.MD5
import com.example.androidmusicplayer.helper.audioExtensions
import com.example.androidmusicplayer.myclasses.FileDirItem
import java.io.File

fun File.isMediaFile() = absolutePath.isMediaFile()
fun File.isAudioFast() = audioExtensions.any { absolutePath.endsWith(it, true) }

fun File.getProperSize(countHiddenItems: Boolean): Long {
    return if (isDirectory) {
        getDirectorySize(this, countHiddenItems)
    } else {
        length()
    }
}

private fun getDirectorySize(dir: File, countHiddenItems: Boolean): Long {
    var size = 0L
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            for (i in files.indices) {
                if (files[i].isDirectory) {
                    size += getDirectorySize(files[i], countHiddenItems)
                } else if (!files[i].name.startsWith('.') && !dir.name.startsWith('.') || countHiddenItems) {
                    size += files[i].length()
                }
            }
        }
    }
    return size
}

fun File.getFileCount(countHiddenItems: Boolean): Int {
    return if (isDirectory) {
        getDirectoryFileCount(this, countHiddenItems)
    } else {
        1
    }
}

private fun getDirectoryFileCount(dir: File, countHiddenItems: Boolean): Int {
    var count = -1
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            count++
            for (i in files.indices) {
                val file = files[i]
                if (file.isDirectory) {
                    count++
                    count += getDirectoryFileCount(file, countHiddenItems)
                } else if (!file.name.startsWith('.') || countHiddenItems) {
                    count++
                }
            }
        }
    }
    return count
}

fun File.getDirectChildrenCount(context: Context, countHiddenItems: Boolean): Int {
    val fileCount = if (context.isRestrictedSAFOnlyRoot(path)) {
        context.getAndroidSAFDirectChildrenCount(
            path,
            countHiddenItems
        )
    } else {
        listFiles()?.filter {
            if (countHiddenItems) {
                true
            } else {
                !it.name.startsWith('.')
            }
        }?.size ?: 0
    }

    return fileCount
}

fun File.toFileDirItem(context: Context) = FileDirItem(absolutePath, name, context.getIsPathDirectory(absolutePath), 0, length(), lastModified())

fun File.getDigest(algorithm: String): String? {
    return try {
        inputStream().getDigest(algorithm)
    } catch (e: Exception) {
        null
    }
}

fun File.md5() = this.getDigest(MD5)