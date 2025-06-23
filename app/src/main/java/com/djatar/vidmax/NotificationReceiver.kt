package com.djatar.vidmax

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.djatar.vidmax.utils.NotificationUtil
import com.yausername.youtubedl_android.YoutubeDL

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, 0)
        val action = intent.getIntExtra(ACTION_KEY, ACTION_CANCEL_TASK)
        if (action == ACTION_CANCEL_TASK) {
            val taskId = intent.getStringExtra(TASK_ID_KEY)
            if (taskId.isNullOrEmpty()) return
            YoutubeDL.getInstance().destroyProcessById(taskId)
            NotificationUtil.cancelNotification(notificationId)
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.djatar.vidmax."

        const val ACTION_CANCEL_TASK = 0

        const val ACTION_KEY = PACKAGE_NAME + "action"
        const val TASK_ID_KEY = PACKAGE_NAME + "taskId"

        const val NOTIFICATION_ID_KEY = PACKAGE_NAME + "notificationId"
    }
}