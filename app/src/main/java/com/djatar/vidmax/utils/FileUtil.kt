package com.djatar.vidmax.utils

import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.annotation.CheckResult
import androidx.core.content.FileProvider
import com.djatar.vidmax.R
import com.djatar.vidmax.VidMaxApp.Companion.context
import java.io.File

object FileUtil {
    fun getDownloadDir(): File {
        val publicDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appName = context.getString(R.string.app_name)
        return File(publicDownload, appName).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getTempDir(): File {
        return File(getDownloadDir(), "tmp").apply {
            if (!exists()) mkdirs()
            File(this, ".nomedia").runCatching {
                createNewFile()
            }.onFailure { it.printStackTrace() }
        }
    }

    fun createViewIntent(path: String?): Intent? {
        if (path == null) return null

        val uri = path.runCatching {
            if (File(this).exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    File(path)
                )
            } else null
        }.getOrNull() ?: return null

        return Intent(Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    @CheckResult
    fun findAndScanMedia(title: String, downloadDir: String): List<String> =
        File(downloadDir)
            .walkTopDown()
            .filter { it.isFile && it.absolutePath.contains(title) }
            .map { it.absolutePath }
            .toMutableList()
            .apply {
                MediaScannerConnection.scanFile(
                    context, this.toList().toTypedArray(),
                    null, null
                )
            }
}