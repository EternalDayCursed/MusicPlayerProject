package com.example.androidmusicplayer.adapter

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.media3.session.MediaController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.CustomActivity
import com.example.androidmusicplayer.databinding.ItemTrackQueueBinding
import com.example.androidmusicplayer.dialog.ConfirmationDialog
import com.example.androidmusicplayer.extension.applyColorFilter
import com.example.androidmusicplayer.extension.beVisibleIf
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getFormattedDuration
import com.example.androidmusicplayer.extension.getProperPrimaryColor
import com.example.androidmusicplayer.extension.getTrackCoverArt
import com.example.androidmusicplayer.extension.highlightTextPart
import com.example.androidmusicplayer.extension.lazySmoothScroll
import com.example.androidmusicplayer.extension.sendCommand
import com.example.androidmusicplayer.extension.setupViewBackground
import com.example.androidmusicplayer.extension.shuffledMediaItemsIndices
import com.example.androidmusicplayer.extension.swap
import com.example.androidmusicplayer.extension.toTrack
import com.example.androidmusicplayer.helper.EXTRA_SHUFFLE_INDICES
import com.example.androidmusicplayer.interfaces.ItemMoveCallback
import com.example.androidmusicplayer.interfaces.ItemTouchHelperContract
import com.example.androidmusicplayer.interfaces.StartReorderDragListener
import com.example.androidmusicplayer.models.Track
import com.example.androidmusicplayer.playback.CustomCommands
import com.example.androidmusicplayer.views.MyRecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller

class QueueAdapter(activity: CustomActivity, items: ArrayList<Track>, var currentTrack: Track? = null, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    BaseMusicAdapter<Track>(items, activity, recyclerView, itemClick), ItemTouchHelperContract, RecyclerViewFastScroller.OnPopupTextUpdate {

    private var startReorderDragListener: StartReorderDragListener

    init {
        setupDragListener(true)

        val touchHelper = ItemTouchHelper(ItemMoveCallback(this))
        touchHelper.attachToRecyclerView(recyclerView)

        startReorderDragListener = object : StartReorderDragListener {
            override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_queue

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrackQueueBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return
        holder.bindView(item, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, item, holder)
        }
        bindViewHolder(holder)
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_remove_from_queue -> removeFromQueue()
            R.id.cab_delete_file -> deleteTracks()
            R.id.cab_share -> shareFiles()
            R.id.cab_add_to_playlist -> addToPlaylist()
            R.id.cab_select_all -> selectAll()
        }
    }

    override fun onActionModeCreated() = notifyDataChanged()

    override fun onActionModeDestroyed() = notifyDataChanged()

    fun updateCurrentTrack() {
        context.withPlayer {
            val track = currentMediaItem?.toTrack()
            if (track != null) {
                val lastTrackId = currentTrack?.mediaStoreId
                currentTrack = track
                val previousIndex = items.indexOfFirst { it.mediaStoreId == lastTrackId }
                val newIndex = items.indexOfFirst { it.mediaStoreId == track.mediaStoreId }
                if (previousIndex != -1 && newIndex != -1) {
                    notifyItemChanged(previousIndex)
                    notifyItemChanged(newIndex)
                    recyclerView.lazySmoothScroll(newIndex)
                }
            }
        }
    }

    private fun removeFromQueue() {
        val positions = ArrayList<Int>()
        val selectedTracks = getSelectedTracks()
        selectedTracks.forEach { track ->
            val position = items.indexOfFirst { it.mediaStoreId == track.mediaStoreId }
            if (position != -1) {
                positions.add(position)
            }
        }

        context.removeQueueItems(selectedTracks) {
            refreshTracksList(positions)
        }
    }

    private fun deleteTracks() {
        ConfirmationDialog(context, "", R.string.delete_song_warning, R.string.ok, R.string.cancel) {
            val positions = ArrayList<Int>()
            val selectedTracks = getSelectedTracks()
            selectedTracks.forEach { track ->
                val position = items.indexOfFirst { it.mediaStoreId == track.mediaStoreId }
                if (position != -1) {
                    positions.add(position)
                }
            }

            context.deleteTracks(selectedTracks) {
                refreshTracksList(positions)
            }
        }
    }

    private fun refreshTracksList(positions: ArrayList<Int>) {
        context.runOnUiThread {
            positions.sortDescending()
            positions.forEach {
                items.removeAt(it)
            }

            removeSelectedItems(positions)
            if (items.isEmpty()) {
                context.finish()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView(view: View, track: Track, holder: ViewHolder) {
        ItemTrackQueueBinding.bind(view).apply {
            root.setupViewBackground(context)
            trackQueueFrame.isSelected = selectedKeys.contains(track.hashCode())
            trackQueueTitle.text = if (textToHighlight.isEmpty()) track.title else track.title.highlightTextPart(textToHighlight, properPrimaryColor)

            arrayOf(trackQueueTitle, trackQueueDuration).forEach {
                val color = if (track.mediaStoreId == currentTrack?.mediaStoreId) {
                    activity.getProperPrimaryColor()
                } else {
                    textColor
                }
                it.setTextColor(color)
            }

            trackQueueDuration.text = track.duration.getFormattedDuration()
            trackQueueDragHandle.beVisibleIf(selectedKeys.isNotEmpty())
            trackQueueDragHandle.applyColorFilter(textColor)
            trackQueueDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startReorderDragListener.requestDrag(holder)
                }
                false
            }

            context.getTrackCoverArt(track) { coverArt ->
                loadImage(trackQueueImage, coverArt, placeholderBig)
            }
        }
    }

    override fun updateItems(newItems: ArrayList<Track>, highlightText: String, forceUpdate: Boolean) {
        context.withPlayer {
            currentTrack = currentMediaItem?.toTrack()
            super.updateItems(newItems, highlightText, forceUpdate)
        }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        items.swap(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        swapMediaItemInQueue(fromPosition, toPosition)
    }

    override fun onRowClear(myViewHolder: ViewHolder?) {}

    override fun onRowSelected(myViewHolder: ViewHolder?) {}

    override fun onChange(position: Int) = items.getOrNull(position)?.getBubbleText(context.config.trackSorting) ?: ""

    /**
     * [MediaController.moveMediaItem] is the proper way to move media items but it doesn't work when shuffle mode is enabled. This method modifies
     * the shuffle order when shuffle mode is enabled and defaults to [MediaController.moveMediaItem] otherwise.
     */
    private fun swapMediaItemInQueue(fromPosition: Int, toPosition: Int) {
        context.withPlayer {
            if (shuffleModeEnabled) {
                val indices = shuffledMediaItemsIndices.toMutableList()
                indices.swap(fromPosition, toPosition)
                sendCommand(
                    command = CustomCommands.SET_SHUFFLE_ORDER,
                    extras = bundleOf(EXTRA_SHUFFLE_INDICES to indices.toIntArray())
                )
            } else {
                moveMediaItem(fromPosition, toPosition)
            }
        }
    }
}
