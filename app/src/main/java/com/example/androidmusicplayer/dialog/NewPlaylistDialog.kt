package com.example.androidmusicplayer.dialog

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.databinding.DialogNewPlaylistBinding
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.getPlaylistIdWithTitle
import com.example.androidmusicplayer.extension.setupDialogStuff
import com.example.androidmusicplayer.extension.showKeyboard
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.value
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.models.Playlist

class NewPlaylistDialog(val activity: Activity, var playlist: Playlist? = null, val callback: (playlistId: Int) -> Unit) {
    private var isNewPlaylist = playlist == null
    private val binding by activity.viewBinding(DialogNewPlaylistBinding::inflate)

    init {
        if (playlist == null) {
            playlist = Playlist(0, "")
        }

        binding.newPlaylistTitle.setText(playlist!!.title)
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                val dialogTitle = if (isNewPlaylist) R.string.create_new_playlist else R.string.rename_playlist
                activity.setupDialogStuff(binding.root, this, dialogTitle) { alertDialog ->
                    alertDialog.showKeyboard(binding.newPlaylistTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = binding.newPlaylistTitle.value
                        ensureBackgroundThread {
                            val playlistIdWithTitle = activity.getPlaylistIdWithTitle(title)
                            var isPlaylistTitleTaken = isNewPlaylist && playlistIdWithTitle != -1
                            if (!isPlaylistTitleTaken) {
                                isPlaylistTitleTaken = !isNewPlaylist && playlist!!.id != playlistIdWithTitle && playlistIdWithTitle != -1
                            }

                            if (title.isEmpty()) {
                                activity.toast(R.string.empty_name)
                                return@ensureBackgroundThread
                            } else if (isPlaylistTitleTaken) {
                                activity.toast(R.string.playlist_name_exists)
                                return@ensureBackgroundThread
                            }

                            playlist!!.title = title

                            val eventTypeId = if (isNewPlaylist) {
                                activity.audioHelper.insertPlaylist(playlist!!).toInt()
                            } else {
                                activity.audioHelper.updatePlaylist(playlist!!)
                                playlist!!.id
                            }

                            if (eventTypeId != -1) {
                                alertDialog.dismiss()
                                callback(eventTypeId)
                            } else {
                                activity.toast(R.string.unknown_error_occurred)
                            }
                        }
                    }
                }
            }
    }
}
