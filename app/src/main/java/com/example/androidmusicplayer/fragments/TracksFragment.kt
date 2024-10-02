package com.example.androidmusicplayer.fragments

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.CustomActivity
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.adapter.TracksAdapter
import com.example.androidmusicplayer.databinding.FragmentTracksBinding
import com.example.androidmusicplayer.dialog.ChangeSortingDialog
import com.example.androidmusicplayer.dialog.PermissionRequiredDialog
import com.example.androidmusicplayer.extension.areSystemAnimationsEnabled
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.beGoneIf
import com.example.androidmusicplayer.extension.beVisibleIf
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getParentPath
import com.example.androidmusicplayer.extension.hideKeyboard
import com.example.androidmusicplayer.extension.mediaScanner
import com.example.androidmusicplayer.extension.openNotificationSettings
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.TAB_TRACKS
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.models.Track
import com.example.androidmusicplayer.models.sortSafely

class TracksFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    private var tracks = ArrayList<Track>()
    private val binding by viewBinding(FragmentTracksBinding::bind)

    override fun setupFragment(activity: BaseCustomActivity) {
        ensureBackgroundThread {
            tracks = context.audioHelper.getAllTracks()

            val excludedFolders = context.config.excludedFolders
            tracks = tracks.filter {
                !excludedFolders.contains(it.path.getParentPath())
            }.toMutableList() as ArrayList<Track>

            activity.runOnUiThread {
                val scanning = activity.mediaScanner.isScanning()
                binding.tracksPlaceholder.text = if (scanning) {
                    context.getString(R.string.loading_files)
                } else {
                    context.getString(R.string.no_items_found)
                }
                binding.tracksPlaceholder.beVisibleIf(tracks.isEmpty())
                val adapter = binding.tracksList.adapter
                if (adapter == null) {
                    TracksAdapter(activity = activity, recyclerView = binding.tracksList, sourceType = TracksAdapter.TYPE_TRACKS, items = tracks) {
                        activity.hideKeyboard()
                        activity.handleNotificationPermission { granted ->
                            if (granted) {
                                val startIndex = tracks.indexOf(it as Track)
                                prepareAndPlay(tracks, startIndex)
                            } else {
                                if (context is Activity) {
                                    PermissionRequiredDialog(
                                        activity,
                                        R.string.allow_notifications_music_player,
                                        { activity.openNotificationSettings() }
                                    )
                                }
                            }
                        }
                    }.apply {
                        binding.tracksList.adapter = this
                    }

                    if (context.areSystemAnimationsEnabled) {
                        binding.tracksList.scheduleLayoutAnimation()
                    }
                } else {
                    (adapter as TracksAdapter).updateItems(tracks)
                }
            }
        }
    }

    override fun finishActMode() {
        getAdapter()?.finishActMode()
    }

    override fun onSearchQueryChanged(text: String) {
        val filtered = tracks.filter {
            it.title.contains(text, true) || ("${it.artist} - ${it.album}").contains(text, true)
        }.toMutableList() as ArrayList<Track>
        getAdapter()?.updateItems(filtered, text)
        binding.tracksPlaceholder.beVisibleIf(filtered.isEmpty())
    }

    override fun onSearchClosed() {
        getAdapter()?.updateItems(tracks)
        binding.tracksPlaceholder.beGoneIf(tracks.isNotEmpty())
    }

    override fun onSortOpen(activity: CustomActivity) {
        ChangeSortingDialog(activity, TAB_TRACKS) {
            val adapter = getAdapter() ?: return@ChangeSortingDialog
            tracks.sortSafely(activity.config.trackSorting)
            adapter.updateItems(tracks, forceUpdate = true)
        }
    }

    override fun setupColors(textColor: Int, adjustedPrimaryColor: Int) {
        binding.tracksPlaceholder.setTextColor(textColor)
        binding.tracksFastscroller.updateColors(adjustedPrimaryColor)
        getAdapter()?.updateColors(textColor)
    }

    private fun getAdapter() = binding.tracksList.adapter as? TracksAdapter
}
