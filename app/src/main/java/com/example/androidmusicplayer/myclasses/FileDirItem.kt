package com.example.androidmusicplayer.myclasses

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.bumptech.glide.signature.ObjectKey
import com.example.androidmusicplayer.extension.formatDate
import com.example.androidmusicplayer.extension.formatSize
import com.example.androidmusicplayer.extension.getAlbum
import com.example.androidmusicplayer.extension.getAndroidSAFDirectChildrenCount
import com.example.androidmusicplayer.extension.getAndroidSAFFileCount
import com.example.androidmusicplayer.extension.getAndroidSAFFileSize
import com.example.androidmusicplayer.extension.getAndroidSAFLastModified
import com.example.androidmusicplayer.extension.getArtist
import com.example.androidmusicplayer.extension.getDirectChildrenCount
import com.example.androidmusicplayer.extension.getDocumentFile
import com.example.androidmusicplayer.extension.getDuration
import com.example.androidmusicplayer.extension.getFastDocumentFile
import com.example.androidmusicplayer.extension.getFileCount
import com.example.androidmusicplayer.extension.getFormattedDuration
import com.example.androidmusicplayer.extension.getItemSize
import com.example.androidmusicplayer.extension.getMediaStoreLastModified
import com.example.androidmusicplayer.extension.getParentPath
import com.example.androidmusicplayer.extension.getProperSize
import com.example.androidmusicplayer.extension.getSizeFromContentUri
import com.example.androidmusicplayer.extension.getTitle
import com.example.androidmusicplayer.extension.isPathOnOTG
import com.example.androidmusicplayer.extension.isRestrictedSAFOnlyRoot
import com.example.androidmusicplayer.extension.normalizeString
import com.example.androidmusicplayer.helper.AlphanumericComparator
import com.example.androidmusicplayer.helper.SORT_BY_DATE_MODIFIED
import com.example.androidmusicplayer.helper.SORT_BY_EXTENSION
import com.example.androidmusicplayer.helper.SORT_BY_NAME
import com.example.androidmusicplayer.helper.SORT_BY_SIZE
import com.example.androidmusicplayer.helper.SORT_DESCENDING
import com.example.androidmusicplayer.helper.SORT_USE_NUMERIC_VALUE
import com.example.androidmusicplayer.helper.isNougatPlus
import java.io.File

open class FileDirItem(
    val path: String,
    val name: String = "",
    var isDirectory: Boolean = false,
    var children: Int = 0,
    var size: Long = 0L,
    var modified: Long = 0L,
    var mediaStoreId: Long = 0L
) :
    Comparable<FileDirItem> {
    companion object {
        var sorting = 0
    }

    override fun toString() =
        "FileDirItem(path=$path, name=$name, isDirectory=$isDirectory, children=$children, size=$size, modified=$modified, mediaStoreId=$mediaStoreId)"

    override fun compareTo(other: FileDirItem): Int {
        return if (isDirectory && !other.isDirectory) {
            -1
        } else if (!isDirectory && other.isDirectory) {
            1
        } else {
            var result: Int
            when {
                sorting and SORT_BY_NAME != 0 -> {
                    result = if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                        AlphanumericComparator().compare(name.normalizeString().lowercase(), other.name.normalizeString().lowercase())
                    } else {
                        name.normalizeString().lowercase().compareTo(other.name.normalizeString().lowercase())
                    }
                }

                sorting and SORT_BY_SIZE != 0 -> result = when {
                    size == other.size -> 0
                    size > other.size -> 1
                    else -> -1
                }

                sorting and SORT_BY_DATE_MODIFIED != 0 -> {
                    result = when {
                        modified == other.modified -> 0
                        modified > other.modified -> 1
                        else -> -1
                    }
                }

                else -> {
                    result = getExtension().lowercase().compareTo(other.getExtension().lowercase())
                }
            }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }
            result
        }
    }

    fun getExtension() = if (isDirectory) name else path.substringAfterLast('.', "")

    fun getBubbleText(context: Context, dateFormat: String? = null, timeFormat: String? = null) = when {
        sorting and SORT_BY_SIZE != 0 -> size.formatSize()
        sorting and SORT_BY_DATE_MODIFIED != 0 -> modified.formatDate(context, dateFormat, timeFormat)
        sorting and SORT_BY_EXTENSION != 0 -> getExtension().lowercase()
        else -> name
    }

    fun getProperSize(context: Context, countHidden: Boolean): Long {
        return when {
            context.isRestrictedSAFOnlyRoot(path) -> context.getAndroidSAFFileSize(path)
            context.isPathOnOTG(path) -> context.getDocumentFile(path)?.getItemSize(countHidden) ?: 0
            isNougatPlus() && path.startsWith("content://") -> {
                try {
                    context.contentResolver.openInputStream(Uri.parse(path))?.available()?.toLong() ?: 0L
                } catch (e: Exception) {
                    context.getSizeFromContentUri(Uri.parse(path))
                }
            }

            else -> File(path).getProperSize(countHidden)
        }
    }

    fun getProperFileCount(context: Context, countHidden: Boolean): Int {
        return when {
            context.isRestrictedSAFOnlyRoot(path) -> context.getAndroidSAFFileCount(path, countHidden)
            context.isPathOnOTG(path) -> context.getDocumentFile(path)?.getFileCount(countHidden) ?: 0
            else -> File(path).getFileCount(countHidden)
        }
    }

    fun getDirectChildrenCount(context: Context, countHiddenItems: Boolean): Int {
        return when {
            context.isRestrictedSAFOnlyRoot(path) -> context.getAndroidSAFDirectChildrenCount(path, countHiddenItems)
            context.isPathOnOTG(path) -> context.getDocumentFile(path)?.listFiles()?.filter { if (countHiddenItems) true else !it.name!!.startsWith(".") }?.size
                ?: 0

            else -> File(path).getDirectChildrenCount(context, countHiddenItems)
        }
    }

    fun getLastModified(context: Context): Long {
        return when {
            context.isRestrictedSAFOnlyRoot(path) -> context.getAndroidSAFLastModified(path)
            context.isPathOnOTG(path) -> context.getFastDocumentFile(path)?.lastModified() ?: 0L
            isNougatPlus() && path.startsWith("content://") -> context.getMediaStoreLastModified(path)
            else -> File(path).lastModified()
        }
    }

    fun getParentPath() = path.getParentPath()

    fun getDuration(context: Context) = context.getDuration(path)?.getFormattedDuration()

    fun getArtist(context: Context) = context.getArtist(path)

    fun getAlbum(context: Context) = context.getAlbum(path)

    fun getTitle(context: Context) = context.getTitle(path)

    fun getSignature(): String {
        val lastModified = if (modified > 1) {
            modified
        } else {
            File(path).lastModified()
        }

        return "$path-$lastModified-$size"
    }

    fun getKey() = ObjectKey(getSignature())

    fun assembleContentUri(): Uri {
        val uri = MediaStore.Files.getContentUri("external")

        return Uri.withAppendedPath(uri, mediaStoreId.toString())
    }
}

