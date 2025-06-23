package com.djatar.vidmax

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.djatar.vidmax.utils.FileUtil
import com.djatar.vidmax.utils.NotificationUtil
import com.google.android.gms.ads.MobileAds
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class VidMaxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        applicationScope = CoroutineScope(SupervisorJob())

        applicationScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Initialize MobileAds")
            MobileAds.initialize(this@VidMaxApp)
        }

        initializeYoutubeDL()

        NotificationUtil.createNotificationChannels()

        videoDownloadDir = FileUtil.getDownloadDir().absolutePath
        audioDownloadDir = File(videoDownloadDir, "Audio").absolutePath
    }

    private fun initializeYoutubeDL() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.init(this@VidMaxApp)
                FFmpeg.init(this@VidMaxApp)
                Aria2c.init(this@VidMaxApp)
            } catch (th: Throwable) {
                th.printStackTrace()
                Log.e("VidMaxApp", "Failed to initialize YoutubeDL", th)
            }
        }
    }

    companion object {
        private const val TAG = "VidMaxApp"

        lateinit var applicationScope: CoroutineScope
        lateinit var videoDownloadDir: String
        lateinit var audioDownloadDir: String
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        var isServiceRunning = false

        private val connection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as DownloadService.DownloadServiceBinder
                isServiceRunning = true
            }
            override fun onServiceDisconnected(arg0: ComponentName) {}
        }

        fun startService() {
            if (isServiceRunning) return
            Intent(context.applicationContext, DownloadService::class.java).also { intent ->
                context.applicationContext.bindService(intent, connection, BIND_AUTO_CREATE)
            }
        }

        fun stopService() {
            if (!isServiceRunning) return
            try {
                isServiceRunning = false
                context.applicationContext.run {
                    unbindService(connection)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}