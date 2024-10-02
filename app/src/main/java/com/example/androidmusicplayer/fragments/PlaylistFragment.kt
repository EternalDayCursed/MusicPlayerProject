package com.example.androidmusicplayer.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.CustomActivity
import com.example.androidmusicplayer.activity.TracksActivity
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.adapter.PlaylistsAdapter
import com.example.androidmusicplayer.databinding.FragmentPlaylistsBinding
import com.example.androidmusicplayer.dialog.ChangeSortingDialog
import com.example.androidmusicplayer.dialog.NewPlaylistDialog
import com.example.androidmusicplayer.extension.areSystemAnimationsEnabled
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.beGoneIf
import com.example.androidmusicplayer.extension.beVisibleIf
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.hideKeyboard
import com.example.androidmusicplayer.extension.mediaScanner
import com.example.androidmusicplayer.extension.underlineText
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.PLAYLIST
import com.example.androidmusicplayer.helper.TAB_PLAYLISTS
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.models.Events
import com.example.androidmusicplayer.models.Playlist
import com.example.androidmusicplayer.models.sortSafely
import com.google.gson.Gson
import org.greenrobot.eventbus.EventBus

class PlaylistsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    private var playlists = ArrayList<Playlist>()
    private val binding by viewBinding(FragmentPlaylistsBinding::bind)

    override fun setupFragment(activity: BaseCustomActivity) {
        binding.playlistsPlaceholder2.underlineText()
        binding.playlistsPlaceholder2.setOnClickListener {
            NewPlaylistDialog(activity) {
                EventBus.getDefault().post(Events.PlaylistsUpdated())
            }
        }

        ensureBackgroundThread {
            val playlists = context.audioHelper.getAllPlaylists()
            playlists.forEach {
                it.trackCount = context.audioHelper.getPlaylistTrackCount(it.id)
            }

            playlists.sortSafely(context.config.playlistSorting)
            this.playlists = playlists

            activity.runOnUiThread {
                val scanning = activity.mediaScanner.isScanning()
                binding.playlistsPlaceholder.text = if (scanning) {
                    context.getString(R.string.loading_files)
                } else {
                    context.getString(R.string.no_items_found)
                }
                binding.playlistsPlaceholder.beVisibleIf(playlists.isEmpty())
                binding.playlistsPlaceholder2.beVisibleIf(playlists.isEmpty() && !scanning)

                val adapter = binding.playlistsList.adapter
                if (adapter == null) {
                    PlaylistsAdapter(activity, playlists, binding.playlistsList) {
                        activity.hideKeyboard()
                        Intent(activity, TracksActivity::class.java).apply {
                            putExtra(PLAYLIST, Gson().toJson(it))
                            activity.startActivity(this)
                        }
                    }.apply {
                        binding.playlistsList.adapter = this
                    }

                    if (context.areSystemAnimationsEnabled) {
                        binding.playlistsList.scheduleLayoutAnimation()
                    }
                } else {
                    (adapter as PlaylistsAdapter).updateItems(playlists)
                }
            }
        }
    }

    override fun finishActMode() {
        getAdapter()?.finishActMode()
    }

    override fun onSearchQueryChanged(text: String) {
        val filtered = playlists.filter { it.title.contains(text, true) }.toMutableList() as ArrayList<Playlist>
        getAdapter()?.updateItems(filtered, text)
        binding.playlistsPlaceholder.beVisibleIf(filtered.isEmpty())
        binding.playlistsPlaceholder2.beVisibleIf(filtered.isEmpty())
    }

    override fun onSearchClosed() {
        getAdapter()?.updateItems(playlists)
        binding.playlistsPlaceholder.beGoneIf(playlists.isNotEmpty())
        binding.playlistsPlaceholder2.beGoneIf(playlists.isNotEmpty())
    }

    override fun onSortOpen(activity: CustomActivity) {
        ChangeSortingDialog(activity, TAB_PLAYLISTS) {
            val adapter = getAdapter() ?: return@ChangeSortingDialog
            playlists.sortSafely(activity.config.playlistSorting)
            adapter.updateItems(playlists, forceUpdate = true)
        }
    }

    override fun setupColors(textColor: Int, adjustedPrimaryColor: Int) {
        binding.playlistsPlaceholder.setTextColor(textColor)
        binding.playlistsPlaceholder2.setTextColor(adjustedPrimaryColor)
        binding.playlistsFastscroller.updateColors(adjustedPrimaryColor)
        getAdapter()?.updateColors(textColor)
    }

    private fun getAdapter() = binding.playlistsList.adapter as? PlaylistsAdapter
}
