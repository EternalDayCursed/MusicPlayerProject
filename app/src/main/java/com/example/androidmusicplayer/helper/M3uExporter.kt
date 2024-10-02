package com.example.androidmusicplayer.helper

import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.base.BaseCustomActivity
import com.example.androidmusicplayer.extension.showErrorToast
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.writeLn
import com.example.androidmusicplayer.models.Track
import java.io.OutputStream

class M3uExporter(val activity: BaseCustomActivity) {
    var failedEvents = 0
    var exportedEvents = 0

    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    fun exportPlaylist(
        outputStream: OutputStream?,
        tracks: ArrayList<Track>,
        callback: (result: ExportResult) -> Unit
    ) {
        if (outputStream == null) {
            callback(ExportResult.EXPORT_FAIL)
            return
        }

        activity.toast(R.string.exporting)

        try {
            outputStream.bufferedWriter().use { out ->
                out.writeLn(M3U_HEADER)
                for (track in tracks) {
                    out.writeLn(M3U_ENTRY + track.duration + M3U_DURATION_SEPARATOR + track.artist + " - " + track.title)
                    out.writeLn(track.path)
                    exportedEvents++
                }
            }
        } catch (e: Exception) {
            failedEvents++
            activity.showErrorToast(e)
        } finally {
            outputStream.close()
        }

        callback(
            when {
                exportedEvents == 0 -> ExportResult.EXPORT_FAIL
                failedEvents > 0 -> ExportResult.EXPORT_PARTIAL
                else -> ExportResult.EXPORT_OK
            }
        )
    }
}
