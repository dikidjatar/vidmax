package com.djatar.vidmax.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.djatar.vidmax.R
import com.djatar.vidmax.VidMaxApp.Companion.context
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val DOWNLOAD_AUDIO = "download_audio"
const val VIDEO_QUALITY = "video_quality"

const val YT_DLP_VERSION = "yt_dlp_version"

@Composable
fun Int.toVideoQualityText(): String {
    return when (this) {
        1 -> "2160p"
        2 -> "1440p"
        3 -> "1080p"
        4 -> "720p"
        5 -> "480p"
        6 -> "360p"
        7 -> stringResource(R.string.lowest_quality)
        else -> stringResource(R.string.best_quality)
    }
}

object Settings {
    private val sharedPref =
        context.getSharedPreferences("download_setting", Context.MODE_PRIVATE)
    private val editor = sharedPref.edit()

    fun putString(key: String, value: String) {
        editor.putString(key, value)
        editor.apply()
    }

    fun putInt(key: String, value: Int) {
        editor.putInt(key, value)
        editor.apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getString(key: String, defValue: String = ""): String =
        sharedPref.getString(key, defValue) ?: defValue

    fun getInt(key: String, defValue: Int = -1): Int =
        sharedPref.getInt(key, defValue)

    fun getBoolean(key: String, defValue: Boolean): Boolean =
        sharedPref.getBoolean(key, defValue)

    suspend fun updateYtdlp(): YoutubeDL.UpdateStatus? =
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().updateYoutubeDL(
                context,
                YoutubeDL.UpdateChannel.STABLE
            ).apply {
                if (this == YoutubeDL.UpdateStatus.DONE) {
                    YoutubeDL.getInstance().version(context)?.let {
                        putString(YT_DLP_VERSION, it)
                    }
                }
            }
        }
}