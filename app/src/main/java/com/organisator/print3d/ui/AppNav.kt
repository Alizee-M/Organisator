@file:OptIn(ExperimentalMaterial3Api::class)

package com.organisator.print3d.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PrintPart
import com.organisator.print3d.ui.screens.DashboardScreen
import com.organisator.print3d.ui.screens.JobDetailScreen
import com.organisator.print3d.ui.screens.JobEditScreen
import com.organisator.print3d.ui.screens.JobsScreen
import com.organisator.print3d.ui.screens.ProjectDetailScreen
import com.organisator.print3d.ui.screens.ProjectEditScreen
import com.organisator.print3d.ui.screens.ProjectsScreen
import com.organisator.print3d.ui.screens.SettingsScreen
import com.organisator.print3d.ui.screens.StatsScreen

private enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Atelier", Icons.Outlined.Home),
    JOBS("jobs", "Plateaux", Icons.Outlined.Inventory2),
    PROJECTS("projects", "Projets", Icons.Outlined.FolderOpen),
    STATS("stats", "Stats", Icons.Outlined.QueryStats)
}

@Composable
fun OrganisatorNavHost(
    viewModel: AppViewModel,
    appVersion: String,
    notificationsAllowed: () -> Boolean,
    exactAlarmsAllowed: () -> Boolean,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    deepLinkJobId: Long?,
    onDeepLinkHandled: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val state by viewModel.state.collectAsState()
    val now by viewModel.now.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val range by viewModel.statsRange.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == currentRoute }

    LaunchedEffect(deepLinkJobId) {
        val id = deepLinkJobId ?: return@LaunchedEffect
        navController.navigate("job/$id")
        onDeepLinkHandled()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (tab != null) {
                TopAppBar(
                    title = { Text("") },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Réglages")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (tab != null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = {
                                if (tab != entry) {
                                    navController.navigate(entry.route) {
                                        popUpTo(Tab.DASHBOARD.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(entry.icon, contentDescription = entry.label) },
                            label = { Text(entry.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = tab != null && tab != Tab.STATS) {
                FloatingActionButton(
                    onClick = {
                        if (tab == Tab.PROJECTS) navController.navigate("projectEdit/0")
                        else navController.navigate("jobEdit/0?projectId=0")
                    }
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Ajouter")
                }
            }
        }
    ) { inner ->
        val direction = LocalLayoutDirection.current
        val listPadding = PaddingValues(
            start = inner.calculateStartPadding(direction) + 16.dp,
            end = inner.calculateEndPadding(direction) + 16.dp,
            top = inner.calculateTopPadding(),
            bottom = inner.calculateBottomPadding() + 88.dp
        )

        NavHost(
            navController = navController,
            startDestination = Tab.DASHBOARD.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Tab.DASHBOARD.route) {
                DashboardScreen(
                    state = state,
                    now = now,
                    onOpenJob = { navController.navigate("job/$it") },
                    onNewJob = { navController.navigate("jobEdit/0?projectId=0") },
                    onSeeAll = { navController.navigate(Tab.JOBS.route) },
                    onQuickStatus = { job, status -> viewModel.setStatus(job, status) },
                    contentPadding = listPadding
                )
            }

            composable(Tab.JOBS.route) {
                JobsScreen(
                    state = state,
                    now = now,
                    onOpenJob = { navController.navigate("job/$it") },
                    onNewJob = { navController.navigate("jobEdit/0?projectId=0") },
                    contentPadding = listPadding
                )
            }

            composable(Tab.PROJECTS.route) {
                ProjectsScreen(
                    state = state,
                    now = now,
                    onOpenProject = { navController.navigate("project/$it") },
                    onNewProject = { navController.navigate("projectEdit/0") },
                    contentPadding = listPadding
                )
            }

            composable(Tab.STATS.route) {
                StatsScreen(
                    stats = stats,
                    settings = state.settings,
                    range = range,
                    onRangeChange = viewModel::setStatsRange,
                    hasData = state.jobs.isNotEmpty(),
                    contentPadding = listPadding
                )
            }

            composable("settings") {
                SettingsScreen(
                    settings = state.settings,
                    appVersion = appVersion,
                    exactAlarmsAllowed = exactAlarmsAllowed(),
                    notificationsAllowed = notificationsAllowed(),
                    onRequestNotifications = onRequestNotifications,
                    onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                    onSave = viewModel::saveSettings,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "job/{jobId}",
                arguments = listOf(navArgument("jobId") { type = NavType.LongType })
            ) { entry ->
                val jobId = entry.arguments?.getLong("jobId") ?: 0L
                val job = state.jobs.firstOrNull { it.id == jobId }
                if (job == null) {
                    MissingScreen("Ce plateau n'existe plus.") { navController.popBackStack() }
                } else {
                    JobDetailScreen(
                        job = job,
                        project = state.projects.firstOrNull { it.id == job.projectId },
                        parts = state.parts.filter { it.jobId == job.id },
                        settings = state.settings,
                        now = now,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("jobEdit/${job.id}?projectId=0") },
                        onStatus = { viewModel.setStatus(job, it) },
                        onOutcome = { viewModel.setPlateOutcome(job, it) },
                        onAddPart = { name, qty ->
                            viewModel.savePart(PrintPart(jobId = job.id, name = name, quantity = qty))
                        },
                        onTogglePart = viewModel::cyclePartStatus,
                        onDeletePart = viewModel::deletePart,
                        onReprint = {
                            viewModel.duplicateForReprint(job) { newId ->
                                navController.navigate("job/$newId") {
                                    popUpTo("job/${job.id}") { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }

            composable(
                route = "jobEdit/{jobId}?projectId={projectId}",
                arguments = listOf(
                    navArgument("jobId") { type = NavType.LongType },
                    navArgument("projectId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { entry ->
                val jobId = entry.arguments?.getLong("jobId") ?: 0L
                val projectId = entry.arguments?.getLong("projectId")?.takeIf { it != 0L }
                val existing = state.jobs.firstOrNull { it.id == jobId }
                JobEditScreen(
                    existing = existing,
                    projects = state.projects,
                    settings = state.settings,
                    preselectedProjectId = projectId,
                    onSave = { job ->
                        viewModel.saveJob(job) { savedId ->
                            if (existing == null) {
                                navController.navigate("job/$savedId") {
                                    popUpTo("jobEdit/{jobId}?projectId={projectId}") { inclusive = true }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    },
                    onDelete = { job ->
                        viewModel.deleteJob(job)
                        navController.popBackStack(Tab.JOBS.route, inclusive = false)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "project/{projectId}",
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { entry ->
                val projectId = entry.arguments?.getLong("projectId") ?: 0L
                val project = state.projects.firstOrNull { it.id == projectId }
                if (project == null) {
                    MissingScreen("Ce projet n'existe plus.") { navController.popBackStack() }
                } else {
                    ProjectDetailScreen(
                        project = project,
                        state = state,
                        now = now,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("projectEdit/${project.id}") },
                        onOpenJob = { navController.navigate("job/$it") },
                        onNewJob = { navController.navigate("jobEdit/0?projectId=${project.id}") }
                    )
                }
            }

            composable(
                route = "projectEdit/{projectId}",
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { entry ->
                val projectId = entry.arguments?.getLong("projectId") ?: 0L
                val existing = state.projects.firstOrNull { it.id == projectId }
                ProjectEditScreen(
                    existing = existing,
                    onSave = { project ->
                        viewModel.saveProject(project) { navController.popBackStack() }
                    },
                    onDelete = { project ->
                        viewModel.deleteProject(project)
                        navController.popBackStack(Tab.PROJECTS.route, inclusive = false)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun MissingScreen(message: String, onBack: () -> Unit) {
    Scaffold { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(message, style = MaterialTheme.typography.titleMedium)
            androidx.compose.material3.TextButton(onClick = onBack) { Text("Retour") }
        }
    }
}

/** Statuts déclenchant une action rapide depuis le tableau de bord. */
internal val QUICK_STATUSES = listOf(JobStatus.EN_COURS, JobStatus.TERMINE, JobStatus.A_REFAIRE)
