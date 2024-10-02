package com.example.androidmusicplayer.extension

import android.os.Bundle
import androidx.media3.session.MediaController
import com.example.androidmusicplayer.playback.CustomCommands

fun MediaController.sendCommand(command: CustomCommands, extras: Bundle = Bundle.EMPTY) = sendCustomCommand(command.sessionCommand, extras)
