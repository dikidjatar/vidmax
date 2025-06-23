package com.djatar.vidmax.download

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    val id: String = "",
    val title: String = "",
    val filename: String? = null,
    @SerialName("webpage_url") val webpageUrl: String? = null,
    @SerialName("original_url") val originalUrl: String? = null,
    val vcodec: String? = null,
)
