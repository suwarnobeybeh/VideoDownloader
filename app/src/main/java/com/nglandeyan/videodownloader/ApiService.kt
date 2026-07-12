package com.nglandeyan.videodownloader

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/v1/social/autolink") // Ujung URL sesuai API Anda
    fun extractVideo(
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") apiHost: String,
        @Body request: VideoRequest // Mengirimkan body {"url": "..."}
    ): Call<VideoResponse>
}