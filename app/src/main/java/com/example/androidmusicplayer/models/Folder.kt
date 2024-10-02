package com.example.androidmusicplayer.models

import com.example.androidmusicplayer.extension.sortSafely
import com.example.androidmusicplayer.helper.AlphanumericComparator
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_TITLE
import com.example.androidmusicplayer.helper.SORT_DESCENDING


data class Folder(val title: String, val trackCount: Int, val path: String) {
    companion object {
        fun getComparator(sorting: Int) = Comparator<Folder> { first, second ->
            var result = when {
                sorting and PLAYER_SORT_BY_TITLE != 0 -> AlphanumericComparator().compare(first.title.lowercase(), second.title.lowercase())
                else -> first.trackCount.compareTo(second.trackCount)
            }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }

            return@Comparator result
        }
    }

    fun getBubbleText(sorting: Int) = when {
        sorting and PLAYER_SORT_BY_TITLE != 0 -> title
        else -> trackCount.toString()
    }
}

fun ArrayList<Folder>.sortSafely(sorting: Int) = sortSafely(Folder.getComparator(sorting))
