package com.example.androidmusicplayer.dialog

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.databinding.DialogExportSettingsBinding
import com.example.androidmusicplayer.extension.baseConfig
import com.example.androidmusicplayer.extension.beGone
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.getDoesFilePathExist
import com.example.androidmusicplayer.extension.getFilenameFromPath
import com.example.androidmusicplayer.extension.humanizePath
import com.example.androidmusicplayer.extension.internalStoragePathFromConfig
import com.example.androidmusicplayer.extension.isAValidFilename
import com.example.androidmusicplayer.extension.setupDialogStuff
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.value

@RequiresApi(Build.VERSION_CODES.R)
class ExportSettingsDialog(
    val activity: BaseCustomActivity, val defaultFilename: String, val hidePath: Boolean,
    callback: (path: String, filename: String) -> Unit
) {
    init {
        val lastUsedFolder = activity.baseConfig.lastExportedSettingsFolder
        var folder = if (lastUsedFolder.isNotEmpty() && activity.getDoesFilePathExist(lastUsedFolder)) {
            lastUsedFolder
        } else {
            activity.internalStoragePathFromConfig
        }

        val view = DialogExportSettingsBinding.inflate(activity.layoutInflater, null, false).apply {
            exportSettingsFilename.setText(defaultFilename.removeSuffix(".txt"))

            if (hidePath) {
                exportSettingsPathHint.beGone()
            } else {
                exportSettingsPath.setText(activity.humanizePath(folder))
                exportSettingsPath.setOnClickListener {
                    FilePickerDialog(activity, folder, false, showFAB = true) {
                        exportSettingsPath.setText(activity.humanizePath(it))
                        folder = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view.root, this, R.string.export_settings) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        var filename = view.exportSettingsFilename.value
                        if (filename.isEmpty()) {
                            activity.toast(R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        filename += ".txt"
                        val newPath = "${folder.trimEnd('/')}/$filename"
                        if (!newPath.getFilenameFromPath().isAValidFilename()) {
                            activity.toast(R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        activity.baseConfig.lastExportedSettingsFolder = folder
                        if (!hidePath && activity.getDoesFilePathExist(newPath)) {
                            val title = String.format(activity.getString(R.string.file_already_exists_overwrite), newPath.getFilenameFromPath())
                            ConfirmationDialog(activity, title) {
                                callback(newPath, filename)
                                alertDialog.dismiss()
                            }
                        } else {
                            callback(newPath, filename)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}