package com.organisator.print3d.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notifications {
    const val CHANNEL_REMINDERS = "rappels_impression"
    const val EXTRA_JOB_ID = "job_id"
    const val EXTRA_JOB_NAME = "job_name"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Rappels d'impression",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Rappels pour lancer ou récupérer un plateau d'impression."
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
