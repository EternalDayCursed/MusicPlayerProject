package com.example.androidmusicplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.androidmusicplayer.interfaces.PlaylistsDao
import com.example.androidmusicplayer.interfaces.QueueItemsDao
import com.example.androidmusicplayer.interfaces.SongsDao
import com.example.androidmusicplayer.models.Playlist
import com.example.androidmusicplayer.models.QueueItem
import com.example.androidmusicplayer.models.Track
import com.example.androidmusicplayer.objects.MyExecutor

@Database(entities = [Track::class, Playlist::class, QueueItem::class, ], version = 1)
abstract class SongsDatabase : RoomDatabase() {

    abstract fun SongsDao(): SongsDao

    abstract fun PlaylistsDao(): PlaylistsDao

    abstract fun QueueItemsDao(): QueueItemsDao

    companion object {
        private var db: SongsDatabase? = null

        fun getInstance(context: Context): SongsDatabase {
            if (db == null) {
                synchronized(SongsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, SongsDatabase::class.java, "songs.db")
                            .setQueryExecutor(MyExecutor.myExecutor)
                            .build()
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }
    }
}
