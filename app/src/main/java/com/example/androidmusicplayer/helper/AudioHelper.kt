package com.example.androidmusicplayer.helper

import android.content.Context
import com.example.androidmusicplayer.extension.addBit
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getParentPath
import com.example.androidmusicplayer.extension.playlistDAO
import com.example.androidmusicplayer.extension.queueDAO
import com.example.androidmusicplayer.extension.tracksDAO
import com.example.androidmusicplayer.inlines.indexOfFirstOrNull
import com.example.androidmusicplayer.models.Folder
import com.example.androidmusicplayer.models.Playlist
import com.example.androidmusicplayer.models.QueueItem
import com.example.androidmusicplayer.models.Track
import com.example.androidmusicplayer.models.sortSafely
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AudioHelper(private val context: Context) {

    private val config = context.config

    fun insertTracks(tracks: List<Track>) {
        context.tracksDAO.insertAll(tracks)
    }

    fun getTrack(mediaStoreId: Long): Track? {
        return context.tracksDAO.getTrackWithMediaStoreId(mediaStoreId)
    }

    fun getAllTracks(): ArrayList<Track> {
        val tracks = context.tracksDAO.getAll()
            .applyProperFilenames(config.showFilename)

        tracks.sortSafely(config.trackSorting)
        return tracks
    }

    fun getAllFolders(): ArrayList<Folder> {
        val tracks = context.audioHelper.getAllTracks()
        val foldersMap = tracks.groupBy { it.folderName }
        val folders = ArrayList<Folder>()
        val excludedFolders = config.excludedFolders
        for ((title, folderTracks) in foldersMap) {
            val path = (folderTracks.firstOrNull()?.path?.getParentPath() ?: "").removeSuffix("/")
            if (excludedFolders.contains(path)) {
                continue
            }

            val folder = Folder(title, folderTracks.size, path)
            folders.add(folder)
        }

        folders.sortSafely(config.folderSorting)
        return folders
    }

    fun getFolderTracks(folder: String): ArrayList<Track> {
        val tracks = context.tracksDAO.getTracksFromFolder(folder)
            .applyProperFilenames(config.showFilename)

        tracks.sortSafely(config.getProperFolderSorting(folder))
        return tracks
    }

    fun updateTrackInfo(newPath: String, artist: String, title: String, oldPath: String) {
        context.tracksDAO.updateSongInfo(newPath, artist, title, oldPath)
    }

    fun deleteTrack(mediaStoreId: Long) {
        context.tracksDAO.removeTrack(mediaStoreId)
    }

    fun deleteTracks(tracks: List<Track>) {
        tracks.forEach {
            deleteTrack(it.mediaStoreId)
        }
    }

    fun insertPlaylist(playlist: Playlist): Long {
        return context.playlistDAO.insert(playlist)
    }

    fun updatePlaylist(playlist: Playlist) {
        context.playlistDAO.update(playlist)
    }

    fun getAllPlaylists(): ArrayList<Playlist> {
        return context.playlistDAO.getAll() as ArrayList<Playlist>
    }

    fun getPlaylistTracks(playlistId: Int): ArrayList<Track> {
        val tracks = context.tracksDAO.getTracksFromPlaylist(playlistId)
            .applyProperFilenames(config.showFilename)

        tracks.sortSafely(config.getProperPlaylistSorting(playlistId))
        return tracks
    }

    fun getPlaylistTrackCount(playlistId: Int): Int {
        return context.tracksDAO.getTracksCountFromPlaylist(playlistId)
    }

    fun updateOrderInPlaylist(playlistId: Int, trackId: Long) {
        context.tracksDAO.updateOrderInPlaylist(playlistId, trackId)
    }

    fun deletePlaylists(playlists: ArrayList<Playlist>) {
        context.playlistDAO.deletePlaylists(playlists)
        playlists.forEach {
            context.tracksDAO.removePlaylistSongs(it.id)
        }
    }

    fun getQueuedTracks(queueItems: List<QueueItem> = context.queueDAO.getAll()): ArrayList<Track> {
        val allTracks = getAllTracks().associateBy { it.mediaStoreId }

        // make sure we fetch the songs in the order they were displayed in
        val tracks = queueItems.mapNotNull { queueItem ->
            val track = allTracks[queueItem.trackId]
            if (track != null) {
                if (queueItem.isCurrent) {
                    track.flags = track.flags.addBit(FLAG_IS_CURRENT)
                }
                track
            } else {
                null
            }
        }

        return tracks as ArrayList<Track>
    }

    /**
     * Executes [callback] with current track as quickly as possible and then proceeds to load the complete queue with all tracks.
     */
    fun getQueuedTracksLazily(callback: (tracks: List<Track>, startIndex: Int, startPositionMs: Long) -> Unit) {
        ensureBackgroundThread {
            var queueItems = context.queueDAO.getAll()
            if (queueItems.isEmpty()) {
                initQueue()
                queueItems = context.queueDAO.getAll()
            }

            val currentItem = context.queueDAO.getCurrent()
            if (currentItem == null) {
                callback(emptyList(), 0, 0)
                return@ensureBackgroundThread
            }

            val currentTrack = getTrack(currentItem.trackId)
            if (currentTrack == null) {
                callback(emptyList(), 0, 0)
                return@ensureBackgroundThread
            }

            // immediately return the current track.
            val startPositionMs = currentItem.lastPosition.seconds.inWholeMilliseconds
            callback(listOf(currentTrack), 0, startPositionMs)

            // return the rest of the queued tracks.
            val queuedTracks = getQueuedTracks(queueItems)
            val currentIndex = queuedTracks.indexOfFirstOrNull { it.mediaStoreId == currentTrack.mediaStoreId } ?: 0
            callback(queuedTracks, currentIndex, startPositionMs)
        }
    }

    fun initQueue(): ArrayList<Track> {
        val tracks = getAllTracks()
        val queueItems = tracks.mapIndexed { index, mediaItem ->
            QueueItem(trackId = mediaItem.mediaStoreId, trackOrder = index, isCurrent = index == 0, lastPosition = 0)
        }

        resetQueue(queueItems)
        return tracks
    }

    fun resetQueue(items: List<QueueItem>, currentTrackId: Long? = null, startPosition: Long? = null) {
        context.queueDAO.deleteAllItems()
        context.queueDAO.insertAll(items)
        if (currentTrackId != null && startPosition != null) {
            val startPositionSeconds = startPosition.milliseconds.inWholeSeconds.toInt()
            context.queueDAO.saveCurrentTrackProgress(currentTrackId, startPositionSeconds)
        } else if (currentTrackId != null) {
            context.queueDAO.saveCurrentTrack(currentTrackId)
        }
    }
}

private fun Collection<Track>.applyProperFilenames(showFilename: Int): ArrayList<Track> {
    return distinctBy { "${it.path}/${it.mediaStoreId}" }
        .onEach { it.title = it.getProperTitle(showFilename) } as ArrayList<Track>
}