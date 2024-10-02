package com.example.androidmusicplayer.interfaces

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/search")
    fun search(@Query("name") searchTerm: String): Call<ResponseBody>

    @GET("/download")
    fun downloadFile(@Query("link") searchTerm: String): Call<ResponseBody>
}