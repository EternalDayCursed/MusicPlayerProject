package com.example.androidmusicplayer.adapter

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.CustomActivity
import com.example.androidmusicplayer.databinding.ItemTrackBinding
import com.example.androidmusicplayer.dialog.ConfirmationDialog
import com.example.androidmusicplayer.dialog.EditDialog
import com.example.androidmusicplayer.extension.beGone
import com.example.androidmusicplayer.extension.beVisible
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getFormattedDuration
import com.example.androidmusicplayer.extension.setupViewBackground
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.models.ListItem
import com.example.androidmusicplayer.models.Track
import com.example.androidmusicplayer.views.MyRecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller

class TracksHeaderAdapter(activity: CustomActivity, items: ArrayList<ListItem>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    BaseMusicAdapter<ListItem>(items, activity, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    private val ITEM_HEADER = 0
    private val ITEM_TRACK = 1

    override val cornerRadius = resources.getDimension(R.dimen.rounded_corner_radius_big).toInt()

    override fun getActionMenuId() = R.menu.cab_tracks_header

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrackBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return
        holder.bindView(item,false,false) { itemView, _ ->
            setupTrack(itemView, item as Track)
        }
        bindViewHolder(holder)
    }

    override fun getItemViewType(position: Int): Int {
        return ITEM_TRACK
    }

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_rename).isVisible = shouldShowRename()
            findItem(R.id.cab_play_next).isVisible = shouldShowPlayNext()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_add_to_playlist -> addToPlaylist()
            R.id.cab_add_to_queue -> addToQueue()
            R.id.cab_properties -> showProperties()
            R.id.cab_delete -> askConfirmDelete()
            R.id.cab_share -> shareFiles()
            R.id.cab_rename -> displayEditDialog()
            R.id.cab_select_all -> selectAll()
            R.id.cab_play_next -> playNextInQueue()
        }
    }

    override fun getSelectableItemCount() = items.size - 1

    override fun getIsItemSelectable(position: Int) = position != 0

    private fun askConfirmDelete() {
        ConfirmationDialog(context) {
            ensureBackgroundThread {
                val positions = ArrayList<Int>()
                val selectedTracks = getSelectedTracks()
                selectedTracks.forEach { track ->
                    val position = items.indexOfFirst { it is Track && it.mediaStoreId == track.mediaStoreId }
                    if (position != -1) {
                        positions.add(position)
                    }
                }

                context.deleteTracks(selectedTracks) {
                    context.runOnUiThread {
                        positions.sortDescending()
                        removeSelectedItems(positions)
                        positions.forEach {
                            items.removeAt(it)
                        }

                        // finish activity if all tracks are deleted
                        if (items.none { it is Track }) {
                            context.finish()
                        }
                    }
                }
            }
        }
    }

    private fun setupTrack(view: View, track: Track) {
        ItemTrackBinding.bind(view).apply {
            root.setupViewBackground(context)
            trackFrame.isSelected = selectedKeys.contains(track.hashCode())
            trackTitle.text = track.title
            trackInfo.beGone()

            arrayOf(trackId, trackTitle, trackDuration).forEach {
                it.setTextColor(textColor)
            }

            trackDuration.text = track.duration.getFormattedDuration()
            trackId.text = track.trackId.toString()
            trackImage.beGone()
            trackId.beVisible()
        }
    }

    override fun onChange(position: Int): CharSequence {
        return when (val listItem = items.getOrNull(position)) {
            is Track -> listItem.getBubbleText(context.config.trackSorting)
            else -> ""
        }
    }

    private fun displayEditDialog() {
        getSelectedTracks().firstOrNull()?.let { selectedTrack ->
            EditDialog(context, selectedTrack) { track ->
                val trackIndex = items.indexOfFirst { (it as? Track)?.mediaStoreId == track.mediaStoreId }
                if (trackIndex != -1) {
                    items[trackIndex] = track
                    notifyItemChanged(trackIndex)
                    finishActMode()
                }

                context.refreshQueueAndTracks(track)
            }
        }
    }
}
