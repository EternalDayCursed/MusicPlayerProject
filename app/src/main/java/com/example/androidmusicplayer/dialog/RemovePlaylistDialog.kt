package com.example.androidmusicplayer.dialog

import android.app.Activity
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.databinding.DialogRemovePlaylistBinding
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.setupDialogStuff
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.models.Playlist

class RemovePlaylistDialog(val activity: Activity, val playlist: Playlist? = null, val callback: (deleteFiles: Boolean) -> Unit) {
    private val binding by activity.viewBinding(DialogRemovePlaylistBinding::inflate)

    init {
        binding.removePlaylistDescription.text = getDescriptionText()
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> callback(binding.removePlaylistCheckbox.isChecked) }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.remove_playlist)
            }
    }

    private fun getDescriptionText(): String {
        return if (playlist == null) {
            activity.getString(R.string.remove_playlist_description)
        } else
            String.format(activity.resources.getString(R.string.remove_playlist_description_placeholder), playlist.title)
    }
}
