package com.djatar.vidmax.download

data class DownloadTaskState(
    val url: String = "",
    val title: String = "",
    val id: String = "",
    val progress: Float = 0f,
    val progressText: String? = null
)
