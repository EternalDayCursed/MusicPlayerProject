package com.example.androidmusicplayer.dialog

import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.databinding.DialogManageVisibleTabsBinding
import com.example.androidmusicplayer.extension.beGone
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.setupDialogStuff
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.TAB_ALBUMS
import com.example.androidmusicplayer.helper.TAB_FOLDERS
import com.example.androidmusicplayer.helper.TAB_PLAYLISTS
import com.example.androidmusicplayer.helper.TAB_TRACKS
import com.example.androidmusicplayer.helper.allTabsMask
import com.example.androidmusicplayer.helper.isQPlus
import com.example.androidmusicplayer.views.MyAppCompatCheckbox

class ManageVisibleTabsDialog(val activity: BaseCustomActivity, val callback: (result: Int) -> Unit) {
    private val binding by activity.viewBinding(DialogManageVisibleTabsBinding::inflate)
    private val tabs = LinkedHashMap<Int, MyAppCompatCheckbox>()

    init {
        tabs.apply {
            put(TAB_PLAYLISTS, binding.manageVisibleTabsPlaylists)
            put(TAB_FOLDERS, binding.manageVisibleTabsFolders)
            put(TAB_ALBUMS, binding.manageVisibleTabsAlbums)
            put(TAB_TRACKS, binding.manageVisibleTabsTracks)
        }

        if (!isQPlus()) {
            tabs.remove(TAB_FOLDERS)
            binding.manageVisibleTabsFolders.beGone()
        }

        val showTabs = activity.config.showTabs
        for ((key, value) in tabs) {
            value.isChecked = showTabs and key != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        for ((key, value) in tabs) {
            if (value.isChecked) {
                result += key
            }
        }

        if (result == 0) {
            result = allTabsMask
        }

        callback(result)
    }
}
