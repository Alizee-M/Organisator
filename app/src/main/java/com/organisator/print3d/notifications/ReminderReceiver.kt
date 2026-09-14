package com.organisator.print3d.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.organisator.print3d.MainActivity
import com.organisator.print3d.R

/** Affiche la notification de rappel quand l'alarme se déclenche. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val jobId = intent.getLongExtra(Notifications.EXTRA_JOB_ID, -1L)
        val jobName = intent.getStringExtra(Notifications.EXTRA_JOB_NAME).orEmpty()
        if (jobId < 0) return

        Notifications.createChannels(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Notifications.EXTRA_JOB_ID, jobId)
        }
        val pending = PendingIntent.getActivity(
            context,
            jobId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Plateau à lancer")
            .setContentText(if (jobName.isBlank()) "Un plateau vous attend." else jobName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (jobName.isBlank()) "Un plateau vous attend."
                else "« $jobName » est prévu maintenant."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (allowed) {
            runCatching {
                NotificationManagerCompat.from(context).notify(jobId.toInt(), notification)
            }
        }
    }
}
