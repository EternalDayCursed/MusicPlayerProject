package com.example.androidmusicplayer.activity

import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.currentMediaItems
import com.example.androidmusicplayer.extension.indexOfTrack
import com.example.androidmusicplayer.extension.indexOfTrackOrNull
import com.example.androidmusicplayer.extension.isReallyPlaying
import com.example.androidmusicplayer.extension.isSameMedia
import com.example.androidmusicplayer.extension.maybePreparePlayer
import com.example.androidmusicplayer.extension.maybeRescanTrackPaths
import com.example.androidmusicplayer.extension.prepareUsingTracks
import com.example.androidmusicplayer.extension.sendCommand
import com.example.androidmusicplayer.extension.toMediaItem
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.togglePlayback
import com.example.androidmusicplayer.helper.EXTRA_NEXT_MEDIA_ID
import com.example.androidmusicplayer.helper.SimpleMediaController
import com.example.androidmusicplayer.helper.ensureBackgroundThread
import com.example.androidmusicplayer.helper.isRPlus
import com.example.androidmusicplayer.models.Events
import com.example.androidmusicplayer.models.Track
import com.example.androidmusicplayer.models.toMediaItems
import com.example.androidmusicplayer.playback.CustomCommands
import com.example.androidmusicplayer.playback.PlaybackService.Companion.updatePlaybackInfo
import org.greenrobot.eventbus.EventBus
import java.io.File
/**
 * Base class for activities that want to control the [Player].
 */
abstract class CustomControllerActivity : CustomActivity(), Player.Listener {
    private lateinit var controller: SimpleMediaController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = SimpleMediaController.getInstance(this)
        maybePreparePlayer()
    }

    override fun onStart() {
        super.onStart()
        controller.addListener(this)
    }

    override fun onStop() {
        super.onStop()
        controller.removeListener(this)
    }

    override fun onResume() {
        super.onResume()
        maybePreparePlayer()
    }

    open fun onPlayerPrepared(success: Boolean) {}

    fun withPlayer(callback: MediaController.() -> Unit) = controller.withController(callback)

    fun prepareAndPlay(tracks: List<Track>, startIndex: Int = 0, startPositionMs: Long = 0, startActivity: Boolean = true) {
        withPlayer {
            if (startActivity) {
                startActivity(
                    Intent(this@CustomControllerActivity, TrackActivity::class.java)
                )
            }

            prepareUsingTracks(tracks = tracks, startIndex = startIndex, startPositionMs = startPositionMs, play = true) { success ->
                if (success) {
                    updatePlaybackInfo(this)
                }
            }
        }
    }

    fun maybePreparePlayer() {
        withPlayer {
            maybePreparePlayer(context = this@CustomControllerActivity, callback = ::onPlayerPrepared)
        }
    }

    fun togglePlayback() = withPlayer { togglePlayback() }

    fun addTracksToQueue(tracks: List<Track>, callback: () -> Unit) {
        withPlayer {
            val currentMediaItemsIds = currentMediaItems.map { it.mediaId }
            val mediaItems = tracks.toMediaItems().filter { it.mediaId !in currentMediaItemsIds }
            addMediaItems(mediaItems)
            callback()
        }
    }

    fun removeQueueItems(tracks: List<Track>, callback: (() -> Unit)? = null) {
        withPlayer {
            var currentItemChanged = false
            tracks.forEach {
                val index = currentMediaItems.indexOfTrackOrNull(it)
                if (index != null) {
                    currentItemChanged = index == currentMediaItemIndex
                    removeMediaItem(index)
                }
            }

            if (currentItemChanged) {
                updatePlaybackInfo(this)
            }

            callback?.invoke()
        }
    }

    fun playNextInQueue(track: Track, callback: () -> Unit) {
        withPlayer {
            sendCommand(
                command = CustomCommands.SET_NEXT_ITEM,
                extras = bundleOf(EXTRA_NEXT_MEDIA_ID to track.mediaStoreId.toString())
            )
            callback()
        }
    }

    fun deleteTracks(tracks: List<Track>, callback: () -> Unit) {
        try {
            audioHelper.deleteTracks(tracks)
        } catch (ignored: Exception) {
        }

        val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        maybeRescanTrackPaths(tracks) { tracksToDelete ->
            if (tracksToDelete.isNotEmpty()) {
                if (isRPlus()) {
                    val uris = tracksToDelete.map { ContentUris.withAppendedId(contentUri, it.mediaStoreId) }
                    deleteSDK30Uris(uris) { success ->
                        if (success) {
                            removeQueueItems(tracksToDelete)
                            EventBus.getDefault().post(Events.RefreshFragments())
                            callback()
                        } else {
                            toast(getString(R.string.unknown_error_occurred))
                        }
                    }
                } else {
                    tracksToDelete.forEach { track ->
                        try {
                            val where = "${MediaStore.Audio.Media._ID} = ?"
                            val args = arrayOf(track.mediaStoreId.toString())
                            contentResolver.delete(contentUri, where, args)
                            File(track.path).delete()
                        } catch (ignored: Exception) {
                        }
                    }

                    removeQueueItems(tracksToDelete)
                    EventBus.getDefault().post(Events.RefreshFragments())
                    callback()
                }
            }
        }
    }

    fun refreshQueueAndTracks(trackToUpdate: Track? = null) {
        ensureBackgroundThread {
            val queuedTracks = audioHelper.getQueuedTracks()
            runOnUiThread {
                withPlayer {
                    // it's not yet directly possible to update metadata without interrupting the playback: https://github.com/androidx/media/issues/33
                    if (trackToUpdate == null || currentMediaItem.isSameMedia(trackToUpdate)) {
                        prepareUsingTracks(tracks = queuedTracks, startIndex = currentMediaItemIndex, startPositionMs = currentPosition, play = isReallyPlaying)
                    } else {
                        val trackIndex = currentMediaItems.indexOfTrack(trackToUpdate)
                        if (trackIndex > 0) {
                            removeMediaItem(trackIndex)
                            addMediaItem(trackIndex, trackToUpdate.toMediaItem())
                        }
                    }
                }
            }
        }

        EventBus.getDefault().post(Events.RefreshTracks())
    }
}
