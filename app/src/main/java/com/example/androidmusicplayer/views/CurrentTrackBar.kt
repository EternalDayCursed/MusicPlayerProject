package com.example.androidmusicplayer.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.provider.MediaStore
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.media3.common.MediaItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.databinding.ViewCurrentTrackBarBinding
import com.example.androidmusicplayer.extension.ensureActivityNotDestroyed
import com.example.androidmusicplayer.extension.fadeIn
import com.example.androidmusicplayer.extension.fadeOut
import com.example.androidmusicplayer.extension.getColoredDrawableWithColor
import com.example.androidmusicplayer.extension.getProperBackgroundColor
import com.example.androidmusicplayer.extension.getProperTextColor
import com.example.androidmusicplayer.extension.getTrackCoverArt
import com.example.androidmusicplayer.extension.toTrack
import com.example.androidmusicplayer.extension.updatePlayPauseIcon
import com.example.androidmusicplayer.extension.viewBinding

class CurrentTrackBar(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    private val binding by viewBinding(ViewCurrentTrackBarBinding::bind)

    fun initialize(togglePlayback: () -> Unit) {
        binding.currentTrackPlayPause.setOnClickListener {
            togglePlayback()
        }
    }

    fun updateColors() {
        background = ColorDrawable(context.getProperBackgroundColor())
        binding.currentTrackLabel.setTextColor(context.getProperTextColor())
    }

    fun updateCurrentTrack(mediaItem: MediaItem?) {
        val track = mediaItem?.toTrack()
        if (track == null) {
            fadeOut()
            return
        } else {
            fadeIn()
        }

        val artist = if (track.artist.trim().isNotEmpty() && track.artist != MediaStore.UNKNOWN_STRING) {
            " • ${track.artist}"
        } else {
            ""
        }

        @SuppressLint("SetTextI18n")
        binding.currentTrackLabel.text = "${track.title}$artist"
        val cornerRadius = resources.getDimension(R.dimen.rounded_corner_radius_small).toInt()
        val currentTrackPlaceholder = resources.getColoredDrawableWithColor(R.drawable.ic_headset, context.getProperTextColor())
        val options = RequestOptions()
            .error(currentTrackPlaceholder)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))

        context.getTrackCoverArt(track) { coverArt ->
            (context as? Activity)?.ensureActivityNotDestroyed {
                Glide.with(this)
                    .load(coverArt)
                    .apply(options)
                    .into(findViewById(R.id.current_track_image))
            }
        }
    }

    fun updateTrackState(isPlaying: Boolean) {
        binding.currentTrackPlayPause.updatePlayPauseIcon(isPlaying, context.getProperTextColor())
    }
}