package com.djatar.vidmax.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.djatar.vidmax.download.DownloadScreen
import com.djatar.vidmax.settings.Settings
import com.djatar.vidmax.settings.YT_DLP_VERSION
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AppEntry"

@Composable
fun AppEntry(paddingValues: PaddingValues) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (Settings.getString(YT_DLP_VERSION, "").isNotEmpty()) {
            return@LaunchedEffect
        }
        runCatching {
            withContext(Dispatchers.IO) {
                val status = Settings.updateYtdlp()
                if (status == YoutubeDL.UpdateStatus.DONE) {
                    withContext(Dispatchers.Main) {
                        val version = Settings.getString(YT_DLP_VERSION)
                        Toast.makeText(context, "Yt-Dlp updated ($version)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.onFailure {
            it.printStackTrace()
            Log.e(TAG, "Failed update YoutubeDL: ${it.message}")
        }
    }

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    DownloadScreen(
        paddingValues = paddingValues,
        onNavigateToSetting = {
            showSettingsDialog = true
        }
    )

    SettingsDialog(
        showDialog = showSettingsDialog,
        onDismiss = { showSettingsDialog = false }
    )
}