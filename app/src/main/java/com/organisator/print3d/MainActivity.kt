package com.organisator.print3d

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organisator.print3d.notifications.Notifications
import com.organisator.print3d.notifications.ReminderScheduler
import com.organisator.print3d.ui.AppViewModel
import com.organisator.print3d.ui.OrganisatorNavHost
import com.organisator.print3d.ui.theme.OrganisatorTheme

class MainActivity : ComponentActivity() {

    private var deepLinkJobId by mutableStateOf<Long?>(null)
    private var notificationsGranted by mutableStateOf(true)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.createChannels(this)
        notificationsGranted = hasNotificationPermission()
        deepLinkJobId = intent.jobIdExtra()

        if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            OrganisatorTheme {
                val vm: AppViewModel = viewModel(factory = AppViewModel.Factory(applicationContext))
                OrganisatorNavHost(
                    viewModel = vm,
                    appVersion = BuildConfig.VERSION_NAME,
                    notificationsAllowed = { notificationsGranted },
                    exactAlarmsAllowed = { ReminderScheduler(this).canScheduleExactAlarms() },
                    onRequestNotifications = { requestNotifications() },
                    onOpenExactAlarmSettings = { openExactAlarmSettings() },
                    deepLinkJobId = deepLinkJobId,
                    onDeepLinkHandled = { deepLinkJobId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.jobIdExtra()?.let { deepLinkJobId = it }
    }

    override fun onResume() {
        super.onResume()
        notificationsGranted = hasNotificationPermission()
    }

    private fun Intent.jobIdExtra(): Long? =
        getLongExtra(Notifications.EXTRA_JOB_ID, -1L).takeIf { it >= 0 }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // La permission a déjà été refusée deux fois : seul l'écran système peut la rendre.
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
        }
    }
}
