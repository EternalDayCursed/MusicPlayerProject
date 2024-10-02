package com.example.androidmusicplayer.activity
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidmusicplayer.R
import com.example.androidmusicplayer.adapter.SongListAdapter
import com.example.androidmusicplayer.databinding.ActivitySearchForMusicBinding
import com.example.androidmusicplayer.extension.config
import com.example.androidmusicplayer.extension.toast
import com.example.androidmusicplayer.extension.viewBinding
import com.example.androidmusicplayer.interfaces.ApiService
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

const val LOG_TAG = "SearchActivity"

class SearchActivity : CustomActivity() {
    private val binding by viewBinding(ActivitySearchForMusicBinding::inflate)
    private lateinit var apiService: ApiService
    private lateinit var adapter: SongListAdapter
    private val data: MutableList<SearchedMusic> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        apiService = createApiService()

        val recyclerView = findViewById<RecyclerView>(R.id.search_result_RV)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        adapter = SongListAdapter(data, this)
        recyclerView.adapter = adapter

        binding.searchButton.setOnClickListener {
            searchSong(binding.songInputEditText.text.toString())
        }
    }

    private fun createApiService(): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(config.serverURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    private fun searchSong(songName: String) {
        Log.d(LOG_TAG, "Search for $songName")

        apiService.search(songName)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        response.body()?.string()?.let { jsonString ->
                            val musicData = processJson(jsonString)
                            adapter.updateList(musicData)
                            Log.d(LOG_TAG, "Updated results: $musicData")
                        } ?: Log.d(LOG_TAG, "Empty response body")
                    } else {
                        Log.d(LOG_TAG, "onResponse: response unsuccessful ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d(LOG_TAG, "onFailure: Error: ${t.message}")
                }
            })
    }

    private fun processJson(jsonString: String): List<SearchedMusic> {
        val musicData = Gson().fromJson(jsonString, MusicData::class.java)
        return musicData.musics.map {
            SearchedMusic(
                musicDuration = it.music_duration,
                musicLink = it.music_link,
                musicName = it.music_name
            )
        }
    }

    data class MusicData(
        @SerializedName("musics")
        val musics: List<JsonMusic>
    )

    data class JsonMusic(
        @SerializedName("music_duration")
        val music_duration: String,
        @SerializedName("music_link")
        val music_link: String,
        @SerializedName("music_name")
        val music_name: String
    )

    data class SearchedMusic(
        val musicDuration: String,
        val musicLink: String,
        val musicName: String
    )

    fun downloadFile(fileUrl: String, fileName: String) {
        val call = apiService.downloadFile(fileUrl)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        if (checkAndRequestStoragePermissions(this@SearchActivity)) {
                            saveFile(this@SearchActivity, responseBody, fileName)
                        }
                        toast("Downloading file with code: ${response.code()}")
                    }
                } else {
                    toast("Response is not successful with code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                toast("Failed to download with error: ${t.message}")
            }
        })
    }

    fun saveFile(context: Context, body: ResponseBody, fileName: String) {
        val contentResolver = context.contentResolver
        val musicCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val newSongDetails = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Laplayer")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val songUri = contentResolver.insert(musicCollection, newSongDetails)

        if (songUri != null) {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = contentResolver.openOutputStream(songUri) as FileOutputStream
                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()

                newSongDetails.clear()
                newSongDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
                contentResolver.update(songUri, newSongDetails, null, null)

                Log.d("SaveFile", "File saved to ${songUri.path}")
            } catch (e: IOException) {
                Log.e("SaveFile", "Error saving the file. Reason: ${e.message}")
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } else {
            Log.e("SaveFile", "Error: could not create MediaStore entry")
        }
    }

    fun checkAndRequestStoragePermissions(activity: Activity): Boolean {
        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        return if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), 1)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Permission granted, proceed with download
                    // Выполняйте здесь сохранение файла после предоставления разрешений, если это необходимо
                    // Например, можно сохранить ссылку и имя файла, а затем вызвать saveFile
                    // saveFile(context, responseBody, fileName)
                } else {
                    // Permission denied, show a message to the user
                    Log.e("Permission", "Required permissions denied")
                }
            }
        }
    }

}