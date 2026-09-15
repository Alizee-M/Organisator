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
import androidx.compose.runtime.collectAsState
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
import com.organisator.print3d.ui.theme.resolveDark

class MainActivity : ComponentActivity() {

    /** Écran à ouvrir au lancement : rappel touché, raccourci, ou lien organisator://. */
    private var pendingRoute by mutableStateOf<String?>(null)
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
        pendingRoute = intent.toRoute()

        if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: AppViewModel = viewModel(factory = AppViewModel.Factory(applicationContext))
            val themeMode by vm.themeMode.collectAsState()
            OrganisatorTheme(darkTheme = themeMode.resolveDark()) {
                OrganisatorNavHost(
                    viewModel = vm,
                    appVersion = BuildConfig.VERSION_NAME,
                    notificationsAllowed = { notificationsGranted },
                    exactAlarmsAllowed = { ReminderScheduler(this).canScheduleExactAlarms() },
                    onRequestNotifications = { requestNotifications() },
                    onOpenExactAlarmSettings = { openExactAlarmSettings() },
                    pendingRoute = pendingRoute,
                    onRouteHandled = { pendingRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toRoute()?.let { pendingRoute = it }
    }

    override fun onResume() {
        super.onResume()
        notificationsGranted = hasNotificationPermission()
    }

    /**
     * Traduit l'intention de lancement en route de navigation. Trois entrées
     * possibles : la notification de rappel, un raccourci du lanceur, ou un lien
     * `organisator://` déclenché par une routine vocale ou une automatisation.
     */
    private fun Intent.toRoute(): String? {
        getLongExtra(Notifications.EXTRA_JOB_ID, -1L)
            .takeIf { it >= 0 }
            ?.let { return "job/$it" }

        val uri = data ?: return null
        if (!uri.scheme.equals(DEEP_LINK_SCHEME, ignoreCase = true)) return null
        val path = (listOfNotNull(uri.host) + uri.pathSegments).joinToString("/")
        return when (path) {
            "plateau/nouveau" -> ROUTE_NEW_JOB
            "projet/nouveau" -> ROUTE_NEW_PROJECT
            else -> null
        }
    }

    private companion object {
        const val DEEP_LINK_SCHEME = "organisator"
        const val ROUTE_NEW_JOB = "jobEdit/0?projectId=0"
        const val ROUTE_NEW_PROJECT = "projectEdit/0"
    }

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
