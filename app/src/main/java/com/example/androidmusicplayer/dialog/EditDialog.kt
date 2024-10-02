package com.example.androidmusicplayer.dialog

import androidx.appcompat.app.AlertDialog
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.databinding.DialogRenameSongBinding
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.beGone
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.getFilenameExtension
import com.example.androidmusicplayer.extension.getFilenameFromPath
import com.example.androidmusicplayer.extension.getParentPath
import com.example.androidmusicplayer.extension.renameFile
import com.example.androidmusicplayer.extension.setupDialogStuff
import com.example.androidmusicplayer.extension.showErrorToast
import com.example.androidmusicplayer.extension.showKeyboard
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.value
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.TagHelper
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.helper.isRPlus
import com.example.androidmusicplayer.models.Track

class EditDialog(val activity: BaseCustomActivity, val track: Track, val callback: (track: Track) -> Unit) {
    private val tagHelper = TagHelper(activity)
    private val binding by activity.viewBinding(DialogRenameSongBinding::inflate)

    init {
        binding.apply {
            title.setText(track.title)
            artist.setText(track.artist)
            album.setText(track.album)
            val filename = track.path.getFilenameFromPath()
            fileName.setText(filename.substring(0, filename.lastIndexOf(".")))
            extension.setText(track.path.getFilenameExtension())
            if (isRPlus()) {
                arrayOf(fileNameHint, extensionHint).forEach {
                    it.beGone()
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.rename_song) { alertDialog ->
                    alertDialog.showKeyboard(binding.title)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.title.value
                        val newArtist = binding.artist.value
                        val newAlbum = binding.album.value
                        val newFilename = binding.fileName.value
                        val newFileExtension = binding.extension.value

                        if (newTitle.isEmpty() || newArtist.isEmpty() || newFilename.isEmpty() || newFileExtension.isEmpty()) {
                            activity.toast(R.string.rename_song_empty)
                            return@setOnClickListener
                        }

                        if (track.title != newTitle || track.artist != newArtist || track.album != newAlbum) {
                            updateContentResolver(track, newArtist, newTitle, newAlbum) {
                                track.artist = newArtist
                                track.title = newTitle
                                track.album = newAlbum
                                val oldPath = track.path
                                val newPath = "${oldPath.getParentPath()}/$newFilename.$newFileExtension"
                                if (oldPath == newPath) {
                                    storeEditedSong(track, oldPath, newPath)
                                    callback(track)
                                    alertDialog.dismiss()
                                    return@updateContentResolver
                                }

                                if (!isRPlus()) {
                                    activity.renameFile(oldPath, newPath, false) { success, _ ->
                                        if (success) {
                                            storeEditedSong(track, oldPath, newPath)
                                            track.path = newPath
                                            callback(track)
                                        } else {
                                            activity.toast(R.string.rename_song_error)
                                        }
                                        alertDialog.dismiss()
                                    }
                                }
                            }
                        } else {
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun storeEditedSong(track: Track, oldPath: String, newPath: String) {
        ensureBackgroundThread {
            try {
                activity.audioHelper.updateTrackInfo(newPath, track.artist, track.title, oldPath)
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }
    }

    private fun updateContentResolver(track: Track, newArtist: String, newTitle: String, newAlbum: String, onUpdateMediaStore: () -> Unit) {
        ensureBackgroundThread {
            try {
                activity.handleRecoverableSecurityException { granted ->
                    if (granted) {
                        tagHelper.writeTag(track, newArtist, newTitle, newAlbum)
                        activity.runOnUiThread {
                            onUpdateMediaStore.invoke()
                        }
                    }
                }
            } catch (e: Exception) {
                activity.toast(R.string.unknown_error_occurred)
            }
        }
    }
}
