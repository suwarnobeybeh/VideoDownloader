package com.nglandeyan.videodownloader

import com.google.gson.annotations.SerializedName

data class VideoRequest(
    val url: String
)

data class VideoResponse(
    @SerializedName("url") val directUrl: String?,
    @SerializedName("medias") val medias: List<MediaItem>?
)

data class MediaItem(
    @SerializedName("url") val url: String?,
    @SerializedName("quality") val quality: String?, // Menangkap info kualitas (e.g., "720p")
    @SerializedName("type") val type: String?       // Menangkap tipe file (video atau audio)
)