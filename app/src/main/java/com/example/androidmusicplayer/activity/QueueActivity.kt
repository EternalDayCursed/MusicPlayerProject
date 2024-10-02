package com.example.androidmusicplayer.activity

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.media3.common.MediaItem
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.adapter.QueueAdapter
import com.example.androidmusicplayer.databinding.ActivityQueueBinding
import com.example.androidmusicplayer.dialog.NewPlaylistDialog
import com.example.androidmusicplayer.extension.areSystemAnimationsEnabled
import com.example.androidmusicplayer.extension.beGoneIf
import com.example.androidmusicplayer.extension.currentMediaItems
import com.example.androidmusicplayer.extension.currentMediaItemsShuffled
import com.example.androidmusicplayer.extension.getProperPrimaryColor
import com.example.androidmusicplayer.extension.indexOfTrack
import com.example.androidmusicplayer.extension.isReallyPlaying
import com.example.androidmusicplayer.extension.lazySmoothScroll
import com.example.androidmusicplayer.extension.shuffledMediaItemsIndices
import com.example.androidmusicplayer.extension.toTrack
import com.example.androidmusicplayer.extension.toTracks
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.helper.NavigationIcon
import com.example.androidmusicplayer.helper.RoomHelper
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.models.Track
class QueueActivity : CustomControllerActivity() {
    private var searchMenuItem: MenuItem? = null
    private var isSearchOpen = false
    private var tracksIgnoringSearch = ArrayList<Track>()

    private val binding by viewBinding(ActivityQueueBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        updateMaterialActivityViews(binding.queueCoordinator, binding.queueList, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.queueList, binding.queueToolbar)

        setupAdapter()
        binding.queueFastscroller.updateColors(getProperPrimaryColor())
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.queueToolbar, NavigationIcon.Arrow, searchMenuItem = searchMenuItem)
    }

    override fun onBackPressed() {
        if (isSearchOpen && searchMenuItem != null) {
            searchMenuItem!!.collapseActionView()
        } else {
            super.onBackPressed()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        getAdapter()?.updateCurrentTrack()
    }

    private fun setupOptionsMenu() {
        setupSearch(binding.queueToolbar.menu)
        binding.queueToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.create_playlist_from_queue -> createPlaylistFromQueue()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        onSearchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                onSearchOpened()
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                onSearchClosed()
                isSearchOpen = false
                return true
            }
        })
    }

    private fun onSearchOpened() {
        val adapter = getAdapter() ?: return
        tracksIgnoringSearch = adapter.items
        adapter.updateItems(tracksIgnoringSearch, forceUpdate = true)
    }

    private fun onSearchClosed() {
        val adapter = getAdapter() ?: return
        adapter.updateItems(tracksIgnoringSearch, forceUpdate = true)
        binding.queuePlaceholder.beGoneIf(tracksIgnoringSearch.isNotEmpty())
    }

    private fun onSearchQueryChanged(text: String) {
        val filtered = tracksIgnoringSearch.filter { it.title.contains(text, true) }.toMutableList() as ArrayList<Track>
        getAdapter()?.updateItems(filtered, text)
        binding.queuePlaceholder.beGoneIf(filtered.isNotEmpty())
    }

    private fun getAdapter(): QueueAdapter? {
        return binding.queueList.adapter as? QueueAdapter
    }

    private fun setupAdapter() {
        if (getAdapter() == null) {
            withPlayer {
                val tracks = currentMediaItemsShuffled.toTracks().toMutableList() as ArrayList<Track>
                binding.queueList.adapter = QueueAdapter(
                    activity = this@QueueActivity,
                    items = tracks,
                    currentTrack = currentMediaItem?.toTrack(),
                    recyclerView = binding.queueList
                ) {
                    withPlayer {
                        val startIndex = currentMediaItems.indexOfTrack(it as Track)
                        seekTo(startIndex, 0)
                        if (!isReallyPlaying) {
                            play()
                        }
                    }
                }

                if (areSystemAnimationsEnabled) {
                    binding.queueList.scheduleLayoutAnimation()
                }

                val currentPosition = shuffledMediaItemsIndices.indexOf(currentMediaItemIndex)
                if (currentPosition > 0) {
                    binding.queueList.lazySmoothScroll(currentPosition)
                }
            }
        }
    }

    private fun createPlaylistFromQueue() {
        NewPlaylistDialog(this) { newPlaylistId ->
            val tracks = ArrayList<Track>()
            getAdapter()?.items?.forEach {
                it.playListId = newPlaylistId
                tracks.add(it)
            }

            ensureBackgroundThread {
                RoomHelper(this).insertTracksWithPlaylist(tracks)
            }
        }
    }
}
