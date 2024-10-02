package com.example.androidmusicplayer.helper

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getArtist
import com.example.androidmusicplayer.extension.getDuration
import com.example.androidmusicplayer.extension.getIntValue
import com.example.androidmusicplayer.extension.getIntValueOrNull
import com.example.androidmusicplayer.extension.getLongValue
import com.example.androidmusicplayer.extension.getStringValue
import com.example.androidmusicplayer.extension.getTitle
import com.example.androidmusicplayer.extension.queryCursor
import com.example.androidmusicplayer.models.Events
import com.example.androidmusicplayer.models.Track
import org.greenrobot.eventbus.EventBus
import java.io.File
import kotlin.math.min

class RoomHelper(val context: Context) {
    fun insertTracksWithPlaylist(tracks: ArrayList<Track>) {
        context.audioHelper.insertTracks(tracks)
        EventBus.getDefault().post(Events.PlaylistsUpdated())
    }

    fun getTrackFromPath(path: String): Track? {
        val songs = getTracksFromPaths(arrayListOf(path), 0)
        return songs.firstOrNull()
    }

    private fun getTracksFromPaths(paths: List<String>, playlistId: Int): ArrayList<Track> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.YEAR
        )

        if (isQPlus()) {
            projection.add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
        }

        if (isRPlus()) {
            projection.add(MediaStore.Audio.Media.GENRE)
            projection.add(MediaStore.Audio.Media.GENRE_ID)
        }

        val pathsMap = HashSet<String>()
        paths.mapTo(pathsMap) { it }

        val ITEMS_PER_GROUP = 50
        val songs = ArrayList<Track>(paths.size)
        val showFilename = context.config.showFilename

        val parts = paths.size / ITEMS_PER_GROUP
        for (i in 0..parts) {
            val sublist = paths.subList(i * ITEMS_PER_GROUP, min((i + 1) * ITEMS_PER_GROUP, paths.size))
            val questionMarks = getQuestionMarks(sublist.size)
            val selection = "${MediaStore.Audio.Media.DATA} IN ($questionMarks)"
            val selectionArgs = sublist.toTypedArray()

            context.queryCursor(uri, projection.toTypedArray(), selection, selectionArgs, showErrors = true) { cursor ->
                val mediaStoreId = cursor.getLongValue(MediaStore.Audio.Media._ID)
                val title = cursor.getStringValue(MediaStore.Audio.Media.TITLE)
                val artist = cursor.getStringValue(MediaStore.Audio.Media.ARTIST)
                val artistId = cursor.getLongValue(MediaStore.Audio.Media.ARTIST_ID)
                val path = cursor.getStringValue(MediaStore.Audio.Media.DATA)
                val duration = cursor.getIntValue(MediaStore.Audio.Media.DURATION) / 1000
                val album = cursor.getStringValue(MediaStore.Audio.Media.ALBUM)
                val albumId = cursor.getLongValue(MediaStore.Audio.Media.ALBUM_ID)
                val coverArt = ContentUris.withAppendedId(artworkUri, albumId).toString()
                val year = cursor.getIntValueOrNull(MediaStore.Audio.Media.YEAR) ?: 0
                val dateAdded = cursor.getIntValueOrNull(MediaStore.Audio.Media.DATE_ADDED) ?: 0
                val folderName = if (isQPlus()) {
                    cursor.getStringValue(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME) ?: MediaStore.UNKNOWN_STRING
                } else {
                    ""
                }

                val genre: String
                val genreId: Long
                if (isRPlus()) {
                    genre = cursor.getStringValue(MediaStore.Audio.Media.GENRE)
                    genreId = cursor.getLongValue(MediaStore.Audio.Media.GENRE_ID)
                } else {
                    genre = ""
                    genreId = 0
                }

                val song = Track(
                    id = 0, mediaStoreId = mediaStoreId, title = title, artist = artist, path = path, duration = duration, album = album, genre = genre,
                    coverArt = coverArt, playListId = playlistId, trackId = 0, folderName = folderName, albumId = albumId, artistId = artistId,
                    genreId = genreId, year = year, dateAdded = dateAdded, orderInPlaylist = 0
                )
                song.title = song.getProperTitle(showFilename)
                songs.add(song)
                pathsMap.remove(path)
            }
        }

        pathsMap.forEach {
            val unknown = MediaStore.UNKNOWN_STRING
            val title = context.getTitle(it) ?: unknown
            val artist = context.getArtist(it) ?: unknown
            val dateAdded = try {
                (File(it).lastModified() / 1000L).toInt()
            } catch (e: Exception) {
                0
            }

            val song = Track(
                id = 0, mediaStoreId = 0, title = title, artist = artist, path = it, duration = context.getDuration(it) ?: 0, album = "",
                genre = "", coverArt = "", playListId = playlistId, trackId = 0, folderName = "", albumId = 0, artistId = 0, genreId = 0,
                year = 0, dateAdded = dateAdded, orderInPlaylist = 0
            )
            song.title = song.getProperTitle(showFilename)
            songs.add(song)
        }

        return songs
    }

    private fun getQuestionMarks(cnt: Int) = "?" + ",?".repeat(Math.max(cnt - 1, 0))
}
