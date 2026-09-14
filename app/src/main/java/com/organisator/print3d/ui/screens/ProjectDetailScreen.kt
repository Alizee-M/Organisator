@file:OptIn(ExperimentalMaterial3Api::class)

package com.organisator.print3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organisator.print3d.ui.theme.AppIcons
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PartStatus
import com.organisator.print3d.data.Project
import com.organisator.print3d.data.effectiveMinutes
import com.organisator.print3d.data.totalCost
import com.organisator.print3d.ui.AppState
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.EmptyState
import com.organisator.print3d.ui.components.JobRow
import com.organisator.print3d.ui.components.ProgressBar
import com.organisator.print3d.ui.components.SectionHeader
import com.organisator.print3d.ui.components.StatTile
import com.organisator.print3d.ui.components.StatusChip
import com.organisator.print3d.util.formatDate
import com.organisator.print3d.util.formatMinutes
import com.organisator.print3d.util.formatMoney
import com.organisator.print3d.util.parseColor

@Composable
fun ProjectDetailScreen(
    project: Project,
    state: AppState,
    now: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenJob: (Long) -> Unit,
    onNewJob: () -> Unit
) {
    val jobs = remember(state.jobs, project.id) {
        state.jobs.filter { it.projectId == project.id }
    }
    val redoByJob = remember(state.parts) {
        state.parts.filter { it.status == PartStatus.A_REFAIRE }
            .groupingBy { it.jobId }.eachCount()
    }
    val color = parseColor(project.colorHex, MaterialTheme.colorScheme.primary)
    val done = jobs.count { it.status == JobStatus.TERMINE }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projet", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modifier")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewJob,
                text = { Text("Plateau") },
                icon = { Icon(AppIcons.Plateau, contentDescription = null) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text(project.name, style = MaterialTheme.typography.headlineSmall)
                    if (project.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            project.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(project.status)
                    }
                }
            }

            item {
                AppCard {
                    SectionHeader("Avancement", subtitle = "$done plateau(x) terminé(s) sur ${jobs.size}")
                    Spacer(Modifier.height(10.dp))
                    ProgressBar(
                        progress = if (jobs.isEmpty()) 0f else done.toFloat() / jobs.size,
                        color = color,
                        height = 8.dp
                    )
                    project.deadline?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Échéance : ${formatDate(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Temps",
                        value = formatMinutes(jobs.sumOf { it.effectiveMinutes(now) }),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Coût",
                        value = formatMoney(
                            jobs.sumOf { it.totalCost(state.settings, now) },
                            state.settings.currency
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader("Plateaux")
            }

            items(jobs, key = { it.id }) { job ->
                JobRow(
                    job = job,
                    projectName = null,
                    settings = state.settings,
                    now = now,
                    onClick = { onOpenJob(job.id) },
                    partsToRedo = redoByJob[job.id] ?: 0
                )
            }

            if (jobs.isEmpty()) {
                item {
                    EmptyState(
                        icon = AppIcons.Plateau,
                        title = "Aucun plateau",
                        message = "Ajoutez un premier plateau à ce projet.",
                        actionLabel = "Nouveau plateau",
                        onAction = onNewJob
                    )
                }
            }

            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}
