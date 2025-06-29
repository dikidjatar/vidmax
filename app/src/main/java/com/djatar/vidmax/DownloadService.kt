package com.djatar.vidmax

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.djatar.vidmax.utils.NotificationUtil
import com.djatar.vidmax.utils.NotificationUtil.SERVICE_NOTIFICATION_ID

private const val TAG = "DownloadService"

class DownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }
        val notification = NotificationUtil.makeServiceNotification(pendingIntent)
        startForeground(SERVICE_NOTIFICATION_ID, notification)
        return DownloadServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: ")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return super.onUnbind(intent)
    }

    inner class DownloadServiceBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }
}