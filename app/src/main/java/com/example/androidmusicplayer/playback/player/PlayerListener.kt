package com.example.androidmusicplayer.playback.player

import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getPlaybackSetting
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.helper.PlaybackSetting
import com.example.androidmusicplayer.playback.PlaybackService

@UnstableApi
internal fun PlaybackService.getPlayerListener() = object : Player.Listener {

    override fun onPlayerError(error: PlaybackException) = toast(R.string.unknown_error_occurred, Toast.LENGTH_LONG)

    override fun onEvents(player: Player, events: Player.Events) {
        if (
            events.containsAny(
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_TRACKS_CHANGED,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_PLAYLIST_METADATA_CHANGED
            )
        ) {
            updatePlaybackState()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // customize repeat mode behaviour as the default behaviour doesn't align with our requirements.
        withPlayer {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                if (config.playbackSetting == PlaybackSetting.STOP_AFTER_CURRENT_TRACK) {
                    seekTo(0)
                    pause()
                }
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        if (config.playbackSetting != PlaybackSetting.STOP_AFTER_CURRENT_TRACK) {
            config.playbackSetting = getPlaybackSetting(repeatMode)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        config.isShuffleEnabled = shuffleModeEnabled
    }
}
