package com.organisator.print3d.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.organisator.print3d.data.PrintRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Les alarmes ne survivent pas au redémarrage : on les réarme au boot. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrintRepository(appContext).rescheduleAllReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
