package com.example.androidmusicplayer.extension

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.TransactionTooLargeException
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsCompat
import com.example.androidmusicplayer.BuildConfig
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.databinding.DialogTitleBinding
import com.example.androidmusicplayer.dialog.PropertiesDialog
import com.example.androidmusicplayer.dialog.SelectPlaylistDialog
import com.example.androidmusicplayer.helper.FLAG_MANUAL_CACHE
import com.example.androidmusicplayer.helper.RoomHelper
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.helper.isOnMainThread
import com.example.androidmusicplayer.helper.isRPlus
import com.example.androidmusicplayer.models.Track
import com.example.androidmusicplayer.myclasses.FileDirItem
import com.example.androidmusicplayer.myclasses.MyContentProvider
import com.example.androidmusicplayer.myclasses.SharedTheme
import com.example.androidmusicplayer.views.MyTextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

fun Activity.appLaunched(appId: String) {
    baseConfig.internalStoragePath = getInternalStoragePath()
    updateSDCardPath()
    baseConfig.appId = appId
    if (baseConfig.appRunCount == 0) {
        baseConfig.wasOrangeIconChecked = true
        checkAppIconColor()
    } else if (!baseConfig.wasOrangeIconChecked) {
        baseConfig.wasOrangeIconChecked = true
        val primaryColor = resources.getColor(R.color.color_primary)
        if (baseConfig.appIconColor != primaryColor) {
            getAppIconColors().forEachIndexed { index, color ->
                toggleAppIconColor(appId, index, color, false)
            }

            val defaultClassName = "${baseConfig.appId.removeSuffix(".debug")}.activities.SplashActivity"
            packageManager.setComponentEnabledSetting(
                ComponentName(baseConfig.appId, defaultClassName),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )

            val orangeClassName = "${baseConfig.appId.removeSuffix(".debug")}.activities.SplashActivity.Orange"
            packageManager.setComponentEnabledSetting(
                ComponentName(baseConfig.appId, orangeClassName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            baseConfig.appIconColor = primaryColor
            baseConfig.lastIconColor = primaryColor
        }
    }
}

fun Activity.updateSharedTheme(sharedTheme: SharedTheme) {
    try {
        val contentValues = MyContentProvider.fillThemeContentValues(sharedTheme)
        applicationContext.contentResolver.update(MyContentProvider.MY_CONTENT_URI, contentValues, null, null)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Activity.showKeyboard(et: EditText) {
    et.requestFocus()
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.addTracksToPlaylist(tracks: List<Track>, callback: () -> Unit) {
    SelectPlaylistDialog(this) { playlistId ->
        val tracksToAdd = ArrayList<Track>()
        tracks.forEach {
            it.id = 0
            it.playListId = playlistId
            tracksToAdd.add(it)
        }

        ensureBackgroundThread {
            RoomHelper(this).insertTracksWithPlaylist(tracksToAdd)

            runOnUiThread {
                callback()
            }
        }
    }
}

fun Activity.maybeRescanTrackPaths(tracks: List<Track>, callback: (tracks: List<Track>) -> Unit) {
    val tracksWithoutId = tracks.filter { it.mediaStoreId == 0L || (it.flags and FLAG_MANUAL_CACHE != 0) }
    if (tracksWithoutId.isNotEmpty()) {
        val pathsToRescan = tracksWithoutId.map { it.path }
        rescanPaths(pathsToRescan) {
            for (track in tracks) {
                if (track.mediaStoreId == 0L || (track.flags and FLAG_MANUAL_CACHE != 0)) {
                    track.mediaStoreId = getMediaStoreIdFromPath(track.path)
                }
            }

            callback(tracks)
        }
    } else {
        callback(tracks)
    }
}

fun Activity.showTrackProperties(selectedTracks: List<Track>) {
    val selectedPaths = selectedTracks.map { track ->
        track.path.ifEmpty {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.mediaStoreId).toString()
        }
    }

    if (selectedPaths.size <= 1) {
        PropertiesDialog(this, selectedPaths.first(), false)
    } else {
        PropertiesDialog(this, selectedPaths, false)
    }
}

fun Activity.ensureActivityNotDestroyed(callback: () -> Unit) {
    if (!isFinishing && !isDestroyed) {
        callback()
    }
}

fun Activity.getFinalUriFromPath(path: String, applicationId: String): Uri? {
    val uri = try {
        ensurePublicUri(path, applicationId)
    } catch (e: Exception) {
        showErrorToast(e)
        return null
    }

    if (uri == null) {
        toast(R.string.unknown_error_occurred)
        return null
    }

    return uri
}


fun Activity.sharePathIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, newUri)
            type = getUriMimeType(path, newUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            grantUriPermission("android", newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(Intent.createChooser(this, getString(R.string.share_via)))
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                } else {
                    showErrorToast(e)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}
fun Activity.sharePathsIntent(paths: List<String>, applicationId: String) {
    ensureBackgroundThread {
        if (paths.size == 1) {
            sharePathIntent(paths.first(), applicationId)
        } else {
            val uriPaths = java.util.ArrayList<String>()
            val newUris = paths.map {
                val uri = getFinalUriFromPath(it, applicationId) ?: return@ensureBackgroundThread
                uriPaths.add(uri.path!!)
                uri
            } as java.util.ArrayList<Uri>

            var mimeType = uriPaths.getMimeType()
            if (mimeType.isEmpty() || mimeType == "*/*") {
                mimeType = paths.getMimeType()
            }

            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, newUris)

                try {
                    startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.no_app_found)
                } catch (e: RuntimeException) {
                    if (e.cause is TransactionTooLargeException) {
                        toast(R.string.maximum_share_reached)
                    } else {
                        showErrorToast(e)
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }
}

fun Activity.shareFiles(tracks: List<Track>) {
    val paths = tracks.map { it.path }
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}

fun Activity.onApplyWindowInsets(callback: (WindowInsetsCompat) -> Unit) {
    window.decorView.setOnApplyWindowInsetsListener { view, insets ->
        callback(WindowInsetsCompat.toWindowInsetsCompat(insets))
        view.onApplyWindowInsets(insets)
        insets
    }
}


fun Activity.hideKeyboard() {
    if (isOnMainThread()) {
        hideKeyboardSync()
    } else {
        Handler(Looper.getMainLooper()).post {
            hideKeyboardSync()
        }
    }
}

fun Activity.hideKeyboardSync() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    currentFocus?.clearFocus()
}

fun Activity.getAlertDialogBuilder() = if (baseConfig.isUsingSystemTheme) {
    MaterialAlertDialogBuilder(this)
} else {
    AlertDialog.Builder(this)
}

fun Activity.isAppInstalledOnSDCard(): Boolean = try {
    val applicationInfo = packageManager.getPackageInfo(packageName, 0).applicationInfo
    (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE
} catch (e: Exception) {
    false
}
fun Activity.setupDialogStuff(
    view: View,
    dialog: AlertDialog.Builder,
    titleId: Int = 0,
    titleText: String = "",
    cancelOnTouchOutside: Boolean = true,
    callback: ((alertDialog: AlertDialog) -> Unit)? = null
) {
    if (isDestroyed || isFinishing) {
        return
    }

    val textColor = getProperTextColor()
    val backgroundColor = getProperBackgroundColor()
    val primaryColor = getProperPrimaryColor()
    if (view is ViewGroup) {
        updateTextColors(view)
    } else if (view is MyTextView) {
        view.setColors(textColor, primaryColor, backgroundColor)
    }

    if (dialog is MaterialAlertDialogBuilder) {
        dialog.create().apply {
            if (titleId != 0) {
                setTitle(titleId)
            } else if (titleText.isNotEmpty()) {
                setTitle(titleText)
            }

            setView(view)
            setCancelable(cancelOnTouchOutside)
            if (!isFinishing) {
                show()
            }
            getButton(Dialog.BUTTON_POSITIVE)?.setTextColor(primaryColor)
            getButton(Dialog.BUTTON_NEGATIVE)?.setTextColor(primaryColor)
            getButton(Dialog.BUTTON_NEUTRAL)?.setTextColor(primaryColor)
            callback?.invoke(this)
        }
    } else {
        var title: DialogTitleBinding? = null
        if (titleId != 0 || titleText.isNotEmpty()) {
            title = DialogTitleBinding.inflate(layoutInflater, null, false)
            title.dialogTitleTextview.apply {
                if (titleText.isNotEmpty()) {
                    text = titleText
                } else {
                    setText(titleId)
                }
                setTextColor(textColor)
            }
        }

        // if we use the same primary and background color, use the text color for dialog confirmation buttons
        val dialogButtonColor = if (primaryColor == baseConfig.backgroundColor) {
            textColor
        } else {
            primaryColor
        }

        dialog.create().apply {
            setView(view)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCustomTitle(title?.root)
            setCanceledOnTouchOutside(cancelOnTouchOutside)
            if (!isFinishing) {
                show()
            }
            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(dialogButtonColor)

            val bgDrawable = when {
                isBlackAndWhiteTheme() -> resources.getDrawable(R.drawable.black_dialog_background, theme)
                baseConfig.isUsingSystemTheme -> resources.getDrawable(R.drawable.dialog_you_background, theme)
                else -> resources.getColoredDrawableWithColor(R.drawable.dialog_bg, baseConfig.backgroundColor)
            }

            window?.setBackgroundDrawable(bgDrawable)
            callback?.invoke(this)
        }
    }
}

fun BaseCustomActivity.renameFile(
    oldPath: String,
    newPath: String,
    isRenamingMultipleFiles: Boolean,
    callback: ((success: Boolean, android30RenameFormat: Android30RenameFormat) -> Unit)? = null
) {
    if (isRestrictedSAFOnlyRoot(oldPath)) {
        handleAndroidSAFDialog(oldPath) {
            if (!it) {
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
                return@handleAndroidSAFDialog
            }

            try {
                ensureBackgroundThread {
                    val success = renameAndroidSAFDocument(oldPath, newPath)
                    runOnUiThread {
                        callback?.invoke(success, Android30RenameFormat.NONE)
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
            }
        }
    } else if (isAccessibleWithSAFSdk30(oldPath)) {
        if (canManageMedia() && !File(oldPath).isDirectory && isPathOnInternalStorage(oldPath)) {
            renameCasually(oldPath, newPath, isRenamingMultipleFiles, callback)
        } else {
            handleSAFDialogSdk30(oldPath) {
                if (!it) {
                    return@handleSAFDialogSdk30
                }

                try {
                    ensureBackgroundThread {
                        val success = renameDocumentSdk30(oldPath, newPath)
                        if (success) {
                            updateInMediaStore(oldPath, newPath)
                            rescanPath(newPath) {
                                runOnUiThread {
                                    callback?.invoke(true, Android30RenameFormat.NONE)
                                }
                                if (!oldPath.equals(newPath, true)) {
                                    deleteFromMediaStore(oldPath)
                                }
                                scanPathRecursively(newPath)
                            }
                        } else {
                            runOnUiThread {
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        }
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                    runOnUiThread {
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        }
    } else if (needsStupidWritePermissions(newPath)) {
        handleSAFDialog(newPath) {
            if (!it) {
                return@handleSAFDialog
            }

            val document = getSomeDocumentFile(oldPath)
            if (document == null || (File(oldPath).isDirectory != document.isDirectory)) {
                runOnUiThread {
                    toast(R.string.unknown_error_occurred)
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
                return@handleSAFDialog
            }

            try {
                ensureBackgroundThread {
                    try {
                        DocumentsContract.renameDocument(applicationContext.contentResolver, document.uri, newPath.getFilenameFromPath())
                    } catch (ignored: FileNotFoundException) {
                        // FileNotFoundException is thrown in some weird cases, but renaming works just fine
                    } catch (e: Exception) {
                        showErrorToast(e)
                        callback?.invoke(false, Android30RenameFormat.NONE)
                        return@ensureBackgroundThread
                    }

                    updateInMediaStore(oldPath, newPath)
                    rescanPaths(arrayListOf(oldPath, newPath)) {
                        if (!baseConfig.keepLastModified) {
                            updateLastModified(newPath, System.currentTimeMillis())
                        }
                        deleteFromMediaStore(oldPath)
                        runOnUiThread {
                            callback?.invoke(true, Android30RenameFormat.NONE)
                        }
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
            }
        }
    } else renameCasually(oldPath, newPath, isRenamingMultipleFiles, callback)
}

enum class Android30RenameFormat {
    SAF,
    CONTENT_RESOLVER,
    NONE
}

fun createTempFile(file: File): File? {
    return if (file.isDirectory) {
        createTempDir("temp", "${System.currentTimeMillis()}", file.parentFile)
    } else {
        if (isRPlus()) {
            // this can throw FileSystemException, lets catch and handle it at the place calling this function
            kotlin.io.path.createTempFile(file.parentFile.toPath(), "temp", "${System.currentTimeMillis()}").toFile()
        } else {
            createTempFile("temp", "${System.currentTimeMillis()}", file.parentFile)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun BaseCustomActivity.renameCasually(
    oldPath: String,
    newPath: String,
    isRenamingMultipleFiles: Boolean,
    callback: ((success: Boolean, android30RenameFormat: Android30RenameFormat) -> Unit)?
) {
    val oldFile = File(oldPath)
    val newFile = File(newPath)
    val tempFile = try {
        createTempFile(oldFile) ?: return
    } catch (exception: Exception) {
        if (isRPlus() && exception is java.nio.file.FileSystemException) {
            // if we are renaming multiple files at once, we should give the Android 30+ permission dialog all uris together, not one by one
            if (isRenamingMultipleFiles) {
                callback?.invoke(false, Android30RenameFormat.CONTENT_RESOLVER)
            } else {
                val fileUris = getFileUrisFromFileDirItems(arrayListOf(File(oldPath).toFileDirItem(this)))
                updateSDK30Uris(fileUris) { success ->
                    if (success) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, newPath.getFilenameFromPath())
                        }

                        try {
                            contentResolver.update(fileUris.first(), values, null, null)
                            callback?.invoke(true, Android30RenameFormat.NONE)
                        } catch (e: Exception) {
                            showErrorToast(e)
                            callback?.invoke(false, Android30RenameFormat.NONE)
                        }
                    } else {
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        } else {
            if (exception is IOException && File(oldPath).isDirectory && isRestrictedWithSAFSdk30(oldPath)) {
                toast(R.string.cannot_rename_folder)
            } else {
                showErrorToast(exception)
            }
            callback?.invoke(false, Android30RenameFormat.NONE)
        }
        return
    }

    val oldToTempSucceeds = oldFile.renameTo(tempFile)
    val tempToNewSucceeds = tempFile.renameTo(newFile)
    if (oldToTempSucceeds && tempToNewSucceeds) {
        if (newFile.isDirectory) {
            updateInMediaStore(oldPath, newPath)
            rescanPath(newPath) {
                runOnUiThread {
                    callback?.invoke(true, Android30RenameFormat.NONE)
                }
                if (!oldPath.equals(newPath, true)) {
                    deleteFromMediaStore(oldPath)
                }
                scanPathRecursively(newPath)
            }
        } else {
            if (!baseConfig.keepLastModified) {
                newFile.setLastModified(System.currentTimeMillis())
            }
            updateInMediaStore(oldPath, newPath)
            scanPathsRecursively(arrayListOf(newPath)) {
                if (!oldPath.equals(newPath, true)) {
                    deleteFromMediaStore(oldPath)
                }
                runOnUiThread {
                    callback?.invoke(true, Android30RenameFormat.NONE)
                }
            }
        }
    } else {
        tempFile.delete()
        newFile.delete()
        if (isRPlus()) {
            // if we are renaming multiple files at once, we should give the Android 30+ permission dialog all uris together, not one by one
            if (isRenamingMultipleFiles) {
                callback?.invoke(false, Android30RenameFormat.SAF)
            } else {
                val fileUris = getFileUrisFromFileDirItems(arrayListOf(File(oldPath).toFileDirItem(this)))
                updateSDK30Uris(fileUris) { success ->
                    if (!success) {
                        return@updateSDK30Uris
                    }
                    try {
                        val sourceUri = fileUris.first()
                        val sourceFile = File(oldPath).toFileDirItem(this)

                        if (oldPath.equals(newPath, true)) {
                            val tempDestination = try {
                                createTempFile(File(sourceFile.path)) ?: return@updateSDK30Uris
                            } catch (exception: Exception) {
                                showErrorToast(exception)
                                callback?.invoke(false, Android30RenameFormat.NONE)
                                return@updateSDK30Uris
                            }

                            val copyTempSuccess = copySingleFileSdk30(sourceFile, tempDestination.toFileDirItem(this))
                            if (copyTempSuccess) {
                                contentResolver.delete(sourceUri, null)
                                tempDestination.renameTo(File(newPath))
                                if (!baseConfig.keepLastModified) {
                                    newFile.setLastModified(System.currentTimeMillis())
                                }
                                updateInMediaStore(oldPath, newPath)
                                scanPathsRecursively(arrayListOf(newPath)) {
                                    runOnUiThread {
                                        callback?.invoke(true, Android30RenameFormat.NONE)
                                    }
                                }
                            } else {
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        } else {
                            val destinationFile = FileDirItem(
                                newPath,
                                newPath.getFilenameFromPath(),
                                sourceFile.isDirectory,
                                sourceFile.children,
                                sourceFile.size,
                                sourceFile.modified
                            )
                            val copySuccessful = copySingleFileSdk30(sourceFile, destinationFile)
                            if (copySuccessful) {
                                if (!baseConfig.keepLastModified) {
                                    newFile.setLastModified(System.currentTimeMillis())
                                }
                                contentResolver.delete(sourceUri, null)
                                updateInMediaStore(oldPath, newPath)
                                scanPathsRecursively(arrayListOf(newPath)) {
                                    runOnUiThread {
                                        callback?.invoke(true, Android30RenameFormat.NONE)
                                    }
                                }
                            } else {
                                toast(R.string.unknown_error_occurred)
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        }

                    } catch (e: Exception) {
                        showErrorToast(e)
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        } else {
            toast(R.string.unknown_error_occurred)
            callback?.invoke(false, Android30RenameFormat.NONE)
        }
    }
}

fun BaseCustomActivity.copySingleFileSdk30(source: FileDirItem, destination: FileDirItem): Boolean {
    val directory = destination.getParentPath()
    if (!createDirectorySync(directory)) {
        val error = String.format(getString(R.string.could_not_create_folder), directory)
        showErrorToast(error)
        return false
    }

    var inputStream: InputStream? = null
    var out: OutputStream? = null
    try {

        out = getFileOutputStreamSync(destination.path, source.path.getMimeType())
        inputStream = getFileInputStreamSync(source.path)!!

        var copiedSize = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = inputStream.read(buffer)
        while (bytes >= 0) {
            out!!.write(buffer, 0, bytes)
            copiedSize += bytes
            bytes = inputStream.read(buffer)
        }

        out?.flush()

        return if (source.size == copiedSize && getDoesFilePathExist(destination.path)) {
            if (baseConfig.keepLastModified) {
                copyOldLastModified(source.path, destination.path)
                val lastModified = File(source.path).lastModified()
                if (lastModified != 0L) {
                    File(destination.path).setLastModified(lastModified)
                }
            }
            true
        } else {
            false
        }
    } finally {
        inputStream?.close()
        out?.close()
    }
}

fun BaseCustomActivity.copyOldLastModified(sourcePath: String, destinationPath: String) {
    val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)
    val uri = MediaStore.Files.getContentUri("external")
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    var selectionArgs = arrayOf(sourcePath)
    val cursor = applicationContext.contentResolver.query(uri, projection, selection, selectionArgs, null)

    cursor?.use {
        if (cursor.moveToFirst()) {
            val dateTaken = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
            val dateModified = cursor.getIntValue(MediaStore.Images.Media.DATE_MODIFIED)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATE_TAKEN, dateTaken)
                put(MediaStore.Images.Media.DATE_MODIFIED, dateModified)
            }

            selectionArgs = arrayOf(destinationPath)
            applicationContext.contentResolver.update(uri, values, selection, selectionArgs)
        }
    }
}

fun Activity.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

private fun BaseCustomActivity.deleteSdk30(fileDirItem: FileDirItem, callback: ((wasSuccess: Boolean) -> Unit)?) {
    val fileUris = getFileUrisFromFileDirItems(arrayListOf(fileDirItem))
    deleteSDK30Uris(fileUris) { success ->
        runOnUiThread {
            callback?.invoke(success)
        }
    }
}

fun BaseCustomActivity.deleteFileBg(
    fileDirItem: FileDirItem,
    allowDeleteFolder: Boolean = false,
    isDeletingMultipleFiles: Boolean,
    callback: ((wasSuccess: Boolean) -> Unit)? = null,
) {
    val path = fileDirItem.path
    if (isRestrictedSAFOnlyRoot(path)) {
        deleteAndroidSAFDirectory(path, allowDeleteFolder, callback)
    } else {
        val file = File(path)
        if (!isRPlus() && file.absolutePath.startsWith(internalStoragePathFromConfig) && !file.canWrite()) {
            callback?.invoke(false)
            return
        }

        var fileDeleted = !isPathOnOTG(path) && ((!file.exists() && file.length() == 0L) || file.delete())
        if (fileDeleted) {
            deleteFromMediaStore(path) { needsRescan ->
                if (needsRescan) {
                    rescanAndDeletePath(path) {
                        runOnUiThread {
                            callback?.invoke(true)
                        }
                    }
                } else {
                    runOnUiThread {
                        callback?.invoke(true)
                    }
                }
            }
        } else {
            if (getIsPathDirectory(file.absolutePath) && allowDeleteFolder) {
                fileDeleted = deleteRecursively(file, this)
            }

            if (!fileDeleted) {
                if (needsStupidWritePermissions(path)) {
                    handleSAFDialog(path) {
                        if (it) {
                            trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
                        }
                    }
                } else if (isAccessibleWithSAFSdk30(path)) {
                    if (canManageMedia()) {
                        deleteSdk30(fileDirItem, callback)
                    } else {
                        handleSAFDialogSdk30(path) {
                            if (it) {
                                deleteDocumentWithSAFSdk30(fileDirItem, allowDeleteFolder, callback)
                            }
                        }
                    }
                } else if (isRPlus() && !isDeletingMultipleFiles) {
                    deleteSdk30(fileDirItem, callback)
                } else {
                    callback?.invoke(false)
                }
            }
        }
    }
}

private fun deleteRecursively(file: File, context: Context): Boolean {
    if (file.isDirectory) {
        val files = file.listFiles() ?: return file.delete()
        for (child in files) {
            deleteRecursively(child, context)
        }
    }

    val deleted = file.delete()
    if (deleted) {
        context.deleteFromMediaStore(file.absolutePath)
    }
    return deleted
}