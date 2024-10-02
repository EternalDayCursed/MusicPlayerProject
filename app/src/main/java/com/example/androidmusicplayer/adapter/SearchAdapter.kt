package com.example.androidmusicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.activity.SearchActivity

class SongListAdapter(private var resultList: MutableList<SearchActivity.SearchedMusic>, private val activity: SearchActivity) : RecyclerView.Adapter<SongListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = resultList[position]
        holder.songName.text = song.musicName
        holder.songDuration.text = song.musicDuration
        holder.searchButton.setOnClickListener {
            activity.downloadFile(song.musicLink, song.musicName)
        }
    }

    override fun getItemCount(): Int {
        return resultList.size
    }

    fun updateList(newList: List<SearchActivity.SearchedMusic>) {
        resultList.clear()
        resultList.addAll(newList ?: listOf())
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songName: TextView = itemView.findViewById(R.id.songName)
        val songDuration: TextView = itemView.findViewById(R.id.songDuration)
        val searchButton: Button = itemView.findViewById(R.id.downloadMusicButton)
    }
}