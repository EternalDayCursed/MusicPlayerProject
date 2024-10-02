package com.example.androidmusicplayer.helper

import android.app.Application
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.extension.audioHelper
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.getFilenameFromPath
import com.example.androidmusicplayer.extension.getIntValue
import com.example.androidmusicplayer.extension.getLongValue
import com.example.androidmusicplayer.extension.getParentPath
import com.example.androidmusicplayer.extension.getStringValue
import com.example.androidmusicplayer.extension.internalStoragePathFromConfig
import com.example.androidmusicplayer.extension.isAudioFast
import com.example.androidmusicplayer.extension.notificationManager
import com.example.androidmusicplayer.extension.queryCursor
import com.example.androidmusicplayer.extension.rescanPaths
import com.example.androidmusicplayer.extension.sdCardPath
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.models.Playlist
import com.example.androidmusicplayer.models.Track
import java.io.File
import java.io.FileInputStream

/**
 * This singleton class manages the process of querying [MediaStore] for new audio files, manually scanning storage for missing audio files, and removing outdated
 * files from the local cache. It ensures that only one scan is running at a time to avoid unnecessary expenses and conflicts.
 */
class SimpleMediaScanner(private val context: Application) {

    private val config = context.config
    private var scanning = false
    private var showProgress = false
    private var onScanComplete: ((complete: Boolean) -> Unit)? = null

    private val mediaStorePaths = arrayListOf<String>()
    private val newTracks = arrayListOf<Track>()

    private var notificationHelper: NotificationHelper? = null
    private var notificationHandler: Handler? = null
    private var lastProgressUpdateMs = 0L

    fun isScanning(): Boolean = scanning

    /**
     * Initiates the scanning process for new audio files, artists, and albums. Since the manual scan can be a slow process, the [callback] parameter is
     * triggered in two stages to ensure that the UI is updated as soon as possible.
     */
    @Synchronized
    fun scan(progress: Boolean = false, callback: ((complete: Boolean) -> Unit)? = null) {
        onScanComplete = callback
        showProgress = progress
        maybeShowScanProgress()

        if (scanning) {
            return
        }

        scanning = true
        ensureBackgroundThread {
            try {
                scanMediaStore()
                if (isQPlus()) {
                    onScanComplete?.invoke(false)
                    scanFilesManually()
                }

                cleanupDatabase()
                onScanComplete?.invoke(true)
            } catch (ignored: Exception) {
            } finally {
                if (showProgress && newTracks.isEmpty()) {
                    context.toast(R.string.no_items_found)
                }

                newTracks.clear()
                mediaStorePaths.clear()
                scanning = false
                hideScanProgress()
            }
        }
    }

    /**
     * Scans [MediaStore] for audio files. Querying [MediaStore.Audio.Artists] and [MediaStore.Audio.Albums] is not necessary in this context, we
     * can manually group tracks by artist and album as done in [scanFilesManually]. However, this approach would require fetching album art bitmaps repeatedly
     * using [MediaMetadataRetriever] instead of utilizing the cached version provided by [MediaStore]. This may become a necessity when we add more nuanced
     * features e.g. group albums by `ALBUM-ARTIST` instead of `ARTIST`
     */
    private fun scanMediaStore() {
        newTracks += getTracksSync()
        mediaStorePaths += newTracks.map { it.path }
        assignGenreToTracks()

        // ignore tracks from excluded folders and tracks with no albums, artists
        val excludedFolders = config.excludedFolders
        val tracksToExclude = mutableSetOf<Track>()
        for (track in newTracks) {
            if (track.path.getParentPath() in excludedFolders) {
                tracksToExclude.add(track)
                continue
            }
        }

        newTracks.removeAll(tracksToExclude)

        updateAllDatabases()
    }

    /**
     * Manually scans the storage for audio files. This method is used to find audio files that may not be available in the [MediaStore] database,
     * as well as files added through unconventional methods (e.g. `adb push`) that may take longer to appear in [MediaStore]. By performing a manual scan,
     * any new audio files can be immediately detected and made visible within the app. Existing paths already available in [MediaStore] are ignored to optimize
     * the scanning process for efficiency.
     */
    private fun scanFilesManually() {
        val trackPaths = newTracks.map { it.path }

        val tracks = findTracksManually(pathsToIgnore = trackPaths)
        if (tracks.isNotEmpty()) {

            newTracks += tracks.filter { it.path !in trackPaths }

            updateAllDatabases()
        }
    }

    private fun updateAllDatabases() {
        context.audioHelper.apply {
            insertTracks(newTracks)
        }
        updateAllTracksPlaylist()
    }

    private fun updateAllTracksPlaylist() {
        if (!config.wasAllTracksPlaylistCreated) {
            val allTracksLabel = context.resources.getString(R.string.all_tracks)
            val playlist = Playlist(ALL_TRACKS_PLAYLIST_ID, allTracksLabel)
            context.audioHelper.insertPlaylist(playlist)
            config.wasAllTracksPlaylistCreated = true
        }

        // avoid re-adding tracks that have been explicitly removed from 'All tracks' playlist
        val excludedFolders = config.excludedFolders
        val tracksRemovedFromAllTracks = config.tracksRemovedFromAllTracksPlaylist.map { it.toLong() }
        val tracksWithPlaylist = newTracks
            .filter { it.mediaStoreId !in tracksRemovedFromAllTracks && it.playListId == 0 && it.path.getParentPath() !in excludedFolders }
            .onEach { it.playListId = ALL_TRACKS_PLAYLIST_ID }
        RoomHelper(context).insertTracksWithPlaylist(tracksWithPlaylist as ArrayList<Track>)
    }

    private fun getTracksSync(): ArrayList<Track> {
        val tracks = arrayListOf<Track>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED
        )

        if (isQPlus()) {
            projection.add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
        }

        if (isRPlus()) {
            projection.add(MediaStore.Audio.Media.GENRE)
            projection.add(MediaStore.Audio.Media.GENRE_ID)
        }

        context.queryCursor(uri, projection.toTypedArray(), showErrors = true) { cursor ->
            val id = cursor.getLongValue(MediaStore.Audio.Media._ID)
            val title = cursor.getStringValue(MediaStore.Audio.Media.TITLE)
            val duration = cursor.getIntValue(MediaStore.Audio.Media.DURATION) / 1000
            val trackId = cursor.getIntValue(MediaStore.Audio.Media.TRACK) % 1000
            val path = cursor.getStringValue(MediaStore.Audio.Media.DATA).orEmpty()
            val artist = cursor.getStringValue(MediaStore.Audio.Media.ARTIST) ?: MediaStore.UNKNOWN_STRING
            val folderName = if (isQPlus()) {
                cursor.getStringValue(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME) ?: MediaStore.UNKNOWN_STRING
            } else {
                ""
            }

            val album = cursor.getStringValue(MediaStore.Audio.Media.ALBUM) ?: folderName
            val albumId = cursor.getLongValue(MediaStore.Audio.Media.ALBUM_ID)
            val artistId = cursor.getLongValue(MediaStore.Audio.Media.ARTIST_ID)
            val year = cursor.getIntValue(MediaStore.Audio.Media.YEAR)
            val dateAdded = cursor.getIntValue(MediaStore.Audio.Media.DATE_ADDED)
            val coverUri = ContentUris.withAppendedId(artworkUri, albumId)
            val coverArt = coverUri.toString()

            val genre: String
            val genreId: Long
            if (isRPlus()) {
                genre = cursor.getStringValue(MediaStore.Audio.Media.GENRE).orEmpty()
                genreId = cursor.getLongValue(MediaStore.Audio.Media.GENRE_ID)
            } else {
                genre = ""
                genreId = 0
            }

            if (!title.isNullOrEmpty()) {
                val track = Track(
                    id = 0, mediaStoreId = id, title = title, artist = artist, path = path, duration = duration, album = album, genre = genre,
                    coverArt = coverArt, playListId = 0, trackId = trackId, folderName = folderName, albumId = albumId, artistId = artistId, genreId = genreId,
                    year = year, dateAdded = dateAdded, orderInPlaylist = 0
                )
                tracks.add(track)
            }
        }

        return tracks
    }

    /**
     * To map tracks to genres, we utilize [MediaStore.Audio.Genres.Members] because [MediaStore.Audio.Media.GENRE_ID] is not available on Android 11 and
     * below. It is essential to call this method after [getTracksSync].
     */
    private fun assignGenreToTracks() {
        if (isRPlus()) {
            return
        }

        val genreToTracks = hashMapOf<Long, MutableList<Long>>()
        val uri = Uri.parse(GENRE_CONTENT_URI)
        val projection = arrayListOf(
            MediaStore.Audio.Genres.Members.GENRE_ID,
            MediaStore.Audio.Genres.Members.AUDIO_ID
        )

        context.queryCursor(uri, projection.toTypedArray(), showErrors = true) {
            val trackId = it.getLongValue(MediaStore.Audio.Genres.Members.AUDIO_ID)
            val genreId = it.getLongValue(MediaStore.Audio.Genres.Members.GENRE_ID)

            var tracks = genreToTracks[genreId]
            if (tracks == null) {
                tracks = mutableListOf(trackId)
            } else {
                tracks.add(trackId)
            }

            genreToTracks[genreId] = tracks
        }

        for ((genreId, trackIds) in genreToTracks) {
            for (track in newTracks) {
                if (track.mediaStoreId in trackIds) {
                    track.genreId = genreId
                }
            }
        }
    }

    private fun findTracksManually(pathsToIgnore: List<String>): ArrayList<Track> {
        val audioFilePaths = arrayListOf<String>()
        val excludedPaths = pathsToIgnore.toMutableList().apply { addAll(0, config.excludedFolders) }

        for (rootPath in arrayOf(context.internalStoragePathFromConfig, context.sdCardPath)) {
            if (rootPath.isEmpty()) {
                continue
            }

            val rootFile = File(rootPath)
            findAudioFiles(rootFile, audioFilePaths, excludedPaths)
        }

        if (audioFilePaths.isEmpty()) {
            return arrayListOf()
        }

        val tracks = arrayListOf<Track>()
        val totalPaths = audioFilePaths.size
        var pathsScanned = 0

        audioFilePaths.forEach { path ->
            pathsScanned += 1
            maybeShowScanProgress(
                pathBeingScanned = path,
                progress = pathsScanned,
                max = totalPaths
            )

            val retriever = MediaMetadataRetriever()
            var inputStream: FileInputStream? = null

            try {
                retriever.setDataSource(path)
            } catch (ignored: Exception) {
                try {
                    inputStream = FileInputStream(path)
                    retriever.setDataSource(inputStream.fd)
                } catch (ignored: Exception) {
                    retriever.release()
                    inputStream?.close()
                    return@forEach
                }
            }

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: path.getFilenameFromPath()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST
            ) ?: MediaStore.UNKNOWN_STRING
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.div(1000)?.toInt() ?: 0
            val folderName = path.getParentPath().getFilenameFromPath()
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: folderName
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val trackId = trackNumber?.split("/")?.first()?.toIntOrNull() ?: 0
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull() ?: 0
            val dateAdded = try {
                (File(path).lastModified() / 1000L).toInt()
            } catch (e: Exception) {
                0
            }

            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty()

            if (title.isNotEmpty()) {
                val track = Track(
                    id = 0, mediaStoreId = 0, title = title, artist = artist, path = path, duration = duration, album = album, genre = genre,
                    coverArt = "", playListId = 0, trackId = trackId, folderName = folderName, albumId = 0, artistId = 0, genreId = 0,
                    year = year, dateAdded = dateAdded, orderInPlaylist = 0, flags = FLAG_MANUAL_CACHE
                )
                // use hashCode() as id for tracking purposes, there's a very slim chance of collision
                track.mediaStoreId = track.hashCode().toLong()
                tracks.add(track)
            }

            try {
                inputStream?.close()
                retriever.release()
            } catch (ignored: Exception) {
            }
        }

        maybeRescanPaths(audioFilePaths)
        return tracks
    }

    private fun findAudioFiles(file: File, destination: ArrayList<String>, excludedPaths: MutableList<String>) {
        if (file.isHidden) {
            return
        }

        val path = file.absolutePath
        if (path in excludedPaths || path.getParentPath() in excludedPaths) {
            return
        }

        if (file.isFile) {
            if (path.isAudioFast()) {
                destination.add(path)
            }
        }
    }

    private fun maybeRescanPaths(paths: ArrayList<String>) {
        val pathsToRescan = paths.filter { path -> path !in mediaStorePaths }
        context.rescanPaths(pathsToRescan)
    }

    private fun cleanupDatabase() {
        // remove invalid tracks
        val newTrackIds = newTracks.map { it.mediaStoreId } as ArrayList<Long>
        val newTrackPaths = newTracks.map { it.path } as ArrayList<String>
        val invalidTracks = context.audioHelper.getAllTracks().filter { it.mediaStoreId !in newTrackIds || it.path !in newTrackPaths }
        context.audioHelper.deleteTracks(invalidTracks)
        newTracks.removeAll(invalidTracks.toSet())
    }

    private fun maybeShowScanProgress(pathBeingScanned: String = "", progress: Int = 0, max: Int = 0) {
        if (!showProgress) {
            return
        }

        if (notificationHandler == null) {
            notificationHandler = Handler(Looper.getMainLooper())
        }

        if (notificationHelper == null) {
            notificationHelper = NotificationHelper.createInstance(context)
        }

        // avoid showing notification for a short duration
        val delayNotification = pathBeingScanned.isEmpty()
        if (delayNotification) {
            notificationHandler?.postDelayed({
                val notification = notificationHelper!!.createMediaScannerNotification(pathBeingScanned, progress, max)
                notificationHelper!!.notify(SCANNER_NOTIFICATION_ID, notification)
            }, SCANNER_NOTIFICATION_DELAY)
        } else {
            if (System.currentTimeMillis() - lastProgressUpdateMs > 100L) {
                lastProgressUpdateMs = System.currentTimeMillis()
                val notification = notificationHelper!!.createMediaScannerNotification(pathBeingScanned, progress, max)
                notificationHelper!!.notify(SCANNER_NOTIFICATION_ID, notification)
            }
        }
    }

    private fun hideScanProgress() {
        if (showProgress) {
            notificationHandler?.removeCallbacksAndMessages(null)
            notificationHandler = null
            context.notificationManager.cancel(SCANNER_NOTIFICATION_ID)
        }
    }

    companion object {
        private const val SCANNER_NOTIFICATION_ID = 43
        private const val SCANNER_NOTIFICATION_DELAY = 1500L
        private const val GENRE_CONTENT_URI = "content://media/external/audio/genres/all/members"

        private var instance: SimpleMediaScanner? = null

        fun getInstance(app: Application): SimpleMediaScanner {
            return if (instance != null) {
                instance!!
            } else {
                instance = SimpleMediaScanner(app)
                instance!!
            }
        }
    }
}
