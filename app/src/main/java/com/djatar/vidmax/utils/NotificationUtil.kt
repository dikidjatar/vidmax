package com.djatar.vidmax.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.djatar.vidmax.NotificationReceiver
import com.djatar.vidmax.NotificationReceiver.Companion.ACTION_CANCEL_TASK
import com.djatar.vidmax.NotificationReceiver.Companion.ACTION_KEY
import com.djatar.vidmax.NotificationReceiver.Companion.NOTIFICATION_ID_KEY
import com.djatar.vidmax.NotificationReceiver.Companion.TASK_ID_KEY
import com.djatar.vidmax.R
import com.djatar.vidmax.VidMaxApp.Companion.context

object NotificationUtil {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private const val PROGRESS_MAX = 100
    private const val PROGRESS_INITIAL = 0
    private const val DOWNLOAD_CHANNEL_ID = "download_notification"
    private const val SERVICE_CHANNEL_ID = "download_service"
    private const val DOWNLOAD_GROUP_ID = "vidmax.download.notification"
    private const val DEFAULT_NOTIFICATION_ID = 100
    const val SERVICE_NOTIFICATION_ID = 123
    private lateinit var serviceNotification: Notification

    fun createNotificationChannels() {
        val groupName = "Downloads"
        val downloadGroup = NotificationChannelGroup(DOWNLOAD_GROUP_ID, groupName)
        notificationManager.createNotificationChannelGroup(downloadGroup)

        val name = "Download"
        val desc = context.getString(R.string.channel_desc)
        val importance = NotificationManager.IMPORTANCE_LOW
        val downloadChannel = NotificationChannel(DOWNLOAD_CHANNEL_ID, name, importance).apply {
            description = desc
            group = DOWNLOAD_GROUP_ID
        }

        val serviceChannel = NotificationChannel(SERVICE_CHANNEL_ID, name, importance).apply {
            description = context.getString(R.string.download_service)
            group = DOWNLOAD_GROUP_ID
        }
        notificationManager.createNotificationChannel(downloadChannel)
        notificationManager.createNotificationChannel(serviceChannel)
    }

    fun notifyProgress(
        title: String,
        notificationId: Int,
        progress: Int = PROGRESS_INITIAL,
        taskId: String? = null,
        text: String? = null
    ) {
        val pendingIntent = taskId?.let {
            Intent(context.applicationContext, NotificationReceiver::class.java)
                .putExtra(TASK_ID_KEY, it)
                .putExtra(NOTIFICATION_ID_KEY, notificationId)
                .putExtra(ACTION_KEY, ACTION_CANCEL_TASK).run {
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        notificationId,
                        this,
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
        }
        val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID).apply {
            setContentTitle(title)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setOnlyAlertOnce(true)
            setProgress(PROGRESS_MAX, progress, progress <= 0)
            setOngoing(true)
            setStyle(NotificationCompat.BigTextStyle().bigText(text))
            pendingIntent?.let {
                addAction(
                    R.drawable.cancel_icon,
                    context.getString(R.string.cancel),
                    it
                )
            }
        }
        notificationManager.notify(notificationId, builder.build())
    }

    fun finishNotification(
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        title: String? = null,
        text: String? = null,
        intent: PendingIntent? = null
    ) {
        notificationManager.cancel(notificationId)
        val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentText(text)
            setOngoing(false)
            setAutoCancel(true)
            title?.let { setContentTitle(it) }
            intent?.let { setContentIntent(it) }
        }
        notificationManager.notify(notificationId, builder.build())
    }

    fun makeServiceNotification(intent: PendingIntent): Notification {
        val title = context.getString(R.string.download_service)
        serviceNotification =
            NotificationCompat.Builder(context, SERVICE_CHANNEL_ID).apply {
                setContentTitle(title)
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentIntent(intent)
                setOngoing(true)
                setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            }.build()
        return serviceNotification
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
}