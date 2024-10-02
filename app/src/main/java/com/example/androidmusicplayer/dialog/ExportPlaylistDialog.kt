package com.example.androidmusicplayer.dialog

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.CustomActivity
import com.example.androidmusicplayer.databinding.DialogExportPlaylistBinding
import com.example.androidmusicplayer.extension.beGone
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.getCurrentFormattedDateTime
import com.example.androidmusicplayer.extension.getParentPath
import com.example.androidmusicplayer.extension.hideKeyboard
import com.example.androidmusicplayer.extension.humanizePath
import com.example.androidmusicplayer.extension.internalStoragePathFromConfig
import com.example.androidmusicplayer.extension.isAValidFilename
import com.example.androidmusicplayer.extension.setupDialogStuff
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.value
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import java.io.File

@RequiresApi(Build.VERSION_CODES.R)
class ExportPlaylistDialog(
    val activity: CustomActivity,
    val path: String,
    val hidePath: Boolean,
    private val callback: (file: File) -> Unit
) {
    private var ignoreClicks = false
    private var realPath = path.ifEmpty { activity.internalStoragePathFromConfig }
    private val binding by activity.viewBinding(DialogExportPlaylistBinding::inflate)

    init {
        binding.apply {
            exportPlaylistFolder.text = activity.humanizePath(realPath)

            val fileName = "playlist_${getCurrentFormattedDateTime()}"
            exportPlaylistFilename.setText(fileName)

            if (hidePath) {
                exportPlaylistFolderLabel.beGone()
                exportPlaylistFolder.beGone()
            } else {
                exportPlaylistFolder.setOnClickListener {
                    activity.hideKeyboard(exportPlaylistFilename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        exportPlaylistFolder.text = activity.humanizePath(it)
                        realPath = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_playlist) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.exportPlaylistFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename.m3u")
                                if (!hidePath && file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ignoreClicks = true
                                ensureBackgroundThread {
                                    activity.config.lastExportPath = file.absolutePath.getParentPath()
                                    callback(file)
                                    alertDialog.dismiss()
                                }
                            }

                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
