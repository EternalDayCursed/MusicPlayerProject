package com.example.androidmusicplayer.dialog

import android.annotation.SuppressLint
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.databinding.DialogCreateNewFolderBinding
import com.example.androidmusicplayer.extension.createAndroidSAFDirectory
import com.example.androidmusicplayer.extension.createSAFDirectorySdk30
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.getDocumentFile
import com.example.androidmusicplayer.extension.getFilenameFromPath
import com.example.androidmusicplayer.extension.getParentPath
import com.example.androidmusicplayer.extension.humanizePath
import com.example.androidmusicplayer.extension.isAStorageRootFolder
import com.example.androidmusicplayer.extension.isAValidFilename
import com.example.androidmusicplayer.extension.isAccessibleWithSAFSdk30
import com.example.androidmusicplayer.extension.isRestrictedSAFOnlyRoot
import com.example.androidmusicplayer.extension.needsStupidWritePermissions
import com.example.androidmusicplayer.extension.setupDialogStuff
import com.example.androidmusicplayer.extension.showErrorToast
import com.example.androidmusicplayer.extension.showKeyboard
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.value
import com.example.androidmusicplayer.helper.isRPlus
import java.io.File


class CreateNewFolderDialog(val activity: BaseCustomActivity, val path: String, val callback: (path: String) -> Unit) {
    init {
        val view = DialogCreateNewFolderBinding.inflate(activity.layoutInflater, null, false)
        view.folderPath.setText("${activity.humanizePath(path).trimEnd('/')}/")

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view.root, this, R.string.create_new_folder) { alertDialog ->
                    alertDialog.showKeyboard(view.folderName)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                        val name = view.folderName.value
                        when {
                            name.isEmpty() -> activity.toast(R.string.empty_name)
                            name.isAValidFilename() -> {
                                val file = File(path, name)
                                if (file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@OnClickListener
                                }

                                createFolder("$path/$name", alertDialog)
                            }

                            else -> activity.toast(R.string.invalid_name)
                        }
                    })
                }
            }
    }

    @SuppressLint("StringFormatInvalid")
    private fun createFolder(path: String, alertDialog: AlertDialog) {
        try {
            when {
                activity.isRestrictedSAFOnlyRoot(path) && activity.createAndroidSAFDirectory(path) -> sendSuccess(alertDialog, path)
                activity.isAccessibleWithSAFSdk30(path) -> activity.handleSAFDialogSdk30(path) {
                    if (it && activity.createSAFDirectorySdk30(path)) {
                        sendSuccess(alertDialog, path)
                    }
                }

                activity.needsStupidWritePermissions(path) -> activity.handleSAFDialog(path) {
                    if (it) {
                        try {
                            val documentFile = activity.getDocumentFile(path.getParentPath())
                            val newDir = documentFile?.createDirectory(path.getFilenameFromPath()) ?: activity.getDocumentFile(path)
                            if (newDir != null) {
                                sendSuccess(alertDialog, path)
                            } else {
                                activity.toast(R.string.unknown_error_occurred)
                            }
                        } catch (e: SecurityException) {
                            activity.showErrorToast(e)
                        }
                    }
                }

                File(path).mkdirs() -> sendSuccess(alertDialog, path)
                isRPlus() && activity.isAStorageRootFolder(path.getParentPath()) -> activity.handleSAFCreateDocumentDialogSdk30(path) {
                    if (it) {
                        sendSuccess(alertDialog, path)
                    }
                }

                else -> activity.toast(activity.getString(R.string.could_not_create_folder, path.getFilenameFromPath()))
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }

    private fun sendSuccess(alertDialog: AlertDialog, path: String) {
        callback(path.trimEnd('/'))
        alertDialog.dismiss()
    }
}