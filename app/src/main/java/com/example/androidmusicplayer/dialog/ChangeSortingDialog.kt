package com.example.androidmusicplayer.dialog

import android.app.Activity
import android.view.ViewGroup
import android.widget.RadioGroup
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.databinding.DialogChangeSortingBinding
import com.example.androidmusicplayer.databinding.SmallRadioButtonBinding
import com.example.androidmusicplayer.extension.beVisibleIf
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getAlertDialogBuilder
import com.example.androidmusicplayer.extension.setupDialogStuff
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.ACTIVITY_PLAYLIST_FOLDER
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_ARTIST_TITLE
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_CUSTOM
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_DATE_ADDED
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_DURATION
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_TITLE
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_TRACK_COUNT
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_TRACK_ID
import com.example.androidmusicplayer.helper.PLAYER_SORT_BY_YEAR
import com.example.androidmusicplayer.helper.SORT_DESCENDING
import com.example.androidmusicplayer.helper.TAB_ALBUMS
import com.example.androidmusicplayer.helper.TAB_FOLDERS
import com.example.androidmusicplayer.helper.TAB_PLAYLISTS
import com.example.androidmusicplayer.helper.TAB_TRACKS
import com.example.androidmusicplayer.models.Playlist
import com.example.androidmusicplayer.myclasses.RadioItem

class ChangeSortingDialog(val activity: Activity, val location: Int, val playlist: Playlist? = null, val path: String? = null, val callback: () -> Unit) {
    private val config = activity.config
    private var currSorting = 0
    private val binding by activity.viewBinding(DialogChangeSortingBinding::inflate)

    init {
        binding.apply {
            sortingDialogUseForThisOnly.beVisibleIf(playlist != null || path != null)

            if (playlist != null) {
                sortingDialogUseForThisOnly.isChecked = config.hasCustomPlaylistSorting(playlist.id)
            } else if (path != null) {
                sortingDialogUseForThisOnly.text = activity.getString(R.string.use_for_this_folder)
                sortingDialogUseForThisOnly.isChecked = config.hasCustomSorting(path)
            }
        }

        currSorting = when (location) {
            TAB_PLAYLISTS -> config.playlistSorting
            TAB_FOLDERS -> config.folderSorting
            TAB_ALBUMS -> config.albumSorting
            TAB_TRACKS -> config.trackSorting
            else -> if (playlist != null) {
                config.getProperPlaylistSorting(playlist.id)
            } else if (path != null) {
                config.getProperFolderSorting(path)
            } else {
                config.trackSorting
            }
        }

        setupSortRadio()
        setupOrderRadio()

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.sort_by)
            }
    }

    private fun setupSortRadio() {
        val radioItems = ArrayList<RadioItem>()
        when (location) {
            TAB_PLAYLISTS, TAB_FOLDERS -> {
                radioItems.add(RadioItem(0, activity.getString(R.string.title), PLAYER_SORT_BY_TITLE))
                radioItems.add(RadioItem(1, activity.getString(R.string.track_count), PLAYER_SORT_BY_TRACK_COUNT))
            }

            TAB_ALBUMS -> {
                radioItems.add(RadioItem(0, activity.getString(R.string.title), PLAYER_SORT_BY_TITLE))
                radioItems.add(RadioItem(1, activity.getString(R.string.artist_name), PLAYER_SORT_BY_ARTIST_TITLE))
                radioItems.add(RadioItem(2, activity.getString(R.string.year), PLAYER_SORT_BY_YEAR))
                radioItems.add(RadioItem(4, activity.getString(R.string.date_added), PLAYER_SORT_BY_DATE_ADDED))
            }

            TAB_TRACKS -> {
                radioItems.add(RadioItem(0, activity.getString(R.string.title), PLAYER_SORT_BY_TITLE))
                radioItems.add(RadioItem(1, activity.getString(R.string.artist), PLAYER_SORT_BY_ARTIST_TITLE))
                radioItems.add(RadioItem(2, activity.getString(R.string.duration), PLAYER_SORT_BY_DURATION))
                radioItems.add(RadioItem(3, activity.getString(R.string.track_number), PLAYER_SORT_BY_TRACK_ID))
                radioItems.add(RadioItem(4, activity.getString(R.string.date_added), PLAYER_SORT_BY_DATE_ADDED))
            }

            ACTIVITY_PLAYLIST_FOLDER -> {
                radioItems.add(RadioItem(0, activity.getString(R.string.title), PLAYER_SORT_BY_TITLE))
                radioItems.add(RadioItem(1, activity.getString(R.string.artist), PLAYER_SORT_BY_ARTIST_TITLE))
                radioItems.add(RadioItem(2, activity.getString(R.string.duration), PLAYER_SORT_BY_DURATION))
                radioItems.add(RadioItem(3, activity.getString(R.string.track_number), PLAYER_SORT_BY_TRACK_ID))
                radioItems.add(RadioItem(4, activity.getString(R.string.date_added), PLAYER_SORT_BY_DATE_ADDED))

                if (playlist != null) {
                    radioItems.add(RadioItem(4, activity.getString(R.string.custom), PLAYER_SORT_BY_CUSTOM))
                }
            }
        }

        binding.sortingDialogRadioSorting.setOnCheckedChangeListener { _, checkedId ->
            binding.sortingOrderDivider.beVisibleIf(checkedId != PLAYER_SORT_BY_CUSTOM)
            binding.sortingDialogRadioOrder.beVisibleIf(checkedId != PLAYER_SORT_BY_CUSTOM)
        }

        radioItems.forEach { radioItem ->
            SmallRadioButtonBinding.inflate(activity.layoutInflater).apply {
                smallRadioButton.apply {
                    text = radioItem.title
                    isChecked = currSorting and (radioItem.value as Int) != 0
                    id = radioItem.value
                }

                binding.sortingDialogRadioSorting.addView(
                    smallRadioButton,
                    RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
            }
        }
    }

    private fun setupOrderRadio() {
        var orderBtn = binding.sortingDialogRadioAscending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = binding.sortingDialogRadioDescending
        }

        orderBtn.isChecked = true
    }

    private fun dialogConfirmed() {
        val sortingRadio = binding.sortingDialogRadioSorting
        var sorting = sortingRadio.checkedRadioButtonId

        if (binding.sortingDialogRadioOrder.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (currSorting != sorting || location == ACTIVITY_PLAYLIST_FOLDER) {
            when (location) {
                TAB_PLAYLISTS -> config.playlistSorting = sorting
                TAB_FOLDERS -> config.folderSorting = sorting
                TAB_ALBUMS -> config.albumSorting = sorting
                TAB_TRACKS -> config.trackSorting = sorting
                ACTIVITY_PLAYLIST_FOLDER -> {
                    if (binding.sortingDialogUseForThisOnly.isChecked) {
                        if (playlist != null) {
                            config.saveCustomPlaylistSorting(playlist.id, sorting)
                        } else if (path != null) {
                            config.saveCustomSorting(path, sorting)
                        }
                    } else {
                        if (playlist != null) {
                            config.removeCustomPlaylistSorting(playlist.id)
                            config.playlistTracksSorting = sorting
                        } else if (path != null) {
                            config.removeCustomSorting(path)
                            config.playlistTracksSorting = sorting
                        } else {
                            config.trackSorting = sorting
                        }
                    }
                }
            }

            callback()
        }
    }
}
