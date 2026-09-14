package com.organisator.print3d.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Programme (ou annule) l'alarme système qui déclenchera le rappel d'un plateau. */
class ReminderScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(jobId: Long, jobName: String, triggerAtMillis: Long) {
        val manager = alarmManager ?: return
        val pending = pendingIntent(jobId, jobName, mutable = false)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else true

        runCatching {
            if (canExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                // Sans la permission d'alarme exacte, on reste approximatif plutôt que
                // de perdre le rappel : Android le déclenchera dans la fenêtre suivante.
                manager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    WINDOW_MILLIS,
                    pending
                )
            }
        }
    }

    fun cancel(jobId: Long) {
        val manager = alarmManager ?: return
        val intent = Intent(appContext, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            appContext,
            jobId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            manager.cancel(pending)
            pending.cancel()
        }
    }

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else true

    private fun pendingIntent(jobId: Long, jobName: String, mutable: Boolean): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(Notifications.EXTRA_JOB_ID, jobId)
            putExtra(Notifications.EXTRA_JOB_NAME, jobName)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(appContext, jobId.toInt(), intent, flags)
    }

    private companion object {
        const val WINDOW_MILLIS = 15 * 60 * 1000L
    }
}
