package com.organisator.print3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.StatsEngine
import com.organisator.print3d.data.StatsRange
import com.organisator.print3d.data.effectiveMinutes
import com.organisator.print3d.data.totalCost
import com.organisator.print3d.ui.AppState
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.EmptyState
import com.organisator.print3d.ui.components.JobRow
import com.organisator.print3d.ui.components.SectionHeader
import com.organisator.print3d.ui.components.StatTile
import com.organisator.print3d.ui.theme.statusColor
import com.organisator.print3d.util.formatMinutes
import com.organisator.print3d.util.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    state: AppState,
    now: Long,
    onOpenJob: (Long) -> Unit,
    onNewJob: () -> Unit,
    onSeeAll: () -> Unit,
    onQuickStatus: (PrintJob, JobStatus) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val jobs = state.jobs
    val projectsById = remember(state.projects) { state.projects.associateBy { it.id } }
    val redoByJob = remember(state.parts) {
        state.parts.filter { it.status == com.organisator.print3d.data.PartStatus.A_REFAIRE }
            .groupingBy { it.jobId }.eachCount()
    }

    val running = remember(jobs) { jobs.filter { it.status == JobStatus.EN_COURS } }
    val queue = remember(jobs) {
        jobs.filter { it.status == JobStatus.A_IMPRIMER }
            .sortedBy { it.scheduledAt ?: Long.MAX_VALUE }
    }
    val todo = remember(jobs) { jobs.filter { it.status == JobStatus.A_FAIRE } }
    val redo = remember(jobs) { jobs.filter { it.status == JobStatus.A_REFAIRE } }

    val weekStats = remember(jobs, state.settings, now / 60_000L) {
        StatsEngine.compute(jobs, state.projects, state.settings, StatsRange.WEEK, now)
    }
    val monthCost = remember(jobs, state.settings, now / 60_000L) {
        StatsEngine.compute(jobs, state.projects, state.settings, StatsRange.MONTH, now).totalCost
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text("Atelier", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "En cours",
                    value = running.size.toString(),
                    caption = running.firstOrNull()?.name,
                    accent = if (running.isNotEmpty()) statusColor(JobStatus.EN_COURS) else null,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "En file",
                    value = queue.size.toString(),
                    caption = if (todo.isNotEmpty()) "+ ${todo.size} à préparer" else "Rien en attente",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "7 derniers jours",
                    value = formatMinutes(weekStats.totalMinutes),
                    caption = "${weekStats.jobs} plateau${if (weekStats.jobs > 1) "x" else ""}",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Coût 30 j",
                    value = formatMoney(monthCost, state.settings.currency),
                    caption = "Filament + énergie",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (running.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                SectionHeader("Sur le plateau", subtitle = "Impressions en cours")
            }
            items(running, key = { "run-${it.id}" }) { job ->
                Column {
                    JobRow(
                        job = job,
                        projectName = projectsById[job.projectId]?.name,
                        settings = state.settings,
                        now = now,
                        onClick = { onOpenJob(job.id) },
                        partsToRedo = redoByJob[job.id] ?: 0
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onQuickStatus(job, JobStatus.TERMINE) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Terminé") }
                        OutlinedButton(
                            onClick = { onQuickStatus(job, JobStatus.A_REFAIRE) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Raté") }
                    }
                }
            }
        }

        if (queue.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                SectionHeader(
                    title = "À imprimer",
                    subtitle = "Les prochains plateaux",
                    actionLabel = if (queue.size > 4) "Tout voir" else null,
                    onAction = onSeeAll
                )
            }
            items(queue.take(4), key = { "queue-${it.id}" }) { job ->
                Column {
                    JobRow(
                        job = job,
                        projectName = projectsById[job.projectId]?.name,
                        settings = state.settings,
                        now = now,
                        onClick = { onOpenJob(job.id) },
                        partsToRedo = redoByJob[job.id] ?: 0
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onQuickStatus(job, JobStatus.EN_COURS) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Lancer l'impression") }
                }
            }
        }

        if (redo.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                SectionHeader("À refaire", subtitle = "Plateaux ou pièces ratés")
            }
            items(redo, key = { "redo-${it.id}" }) { job ->
                JobRow(
                    job = job,
                    projectName = projectsById[job.projectId]?.name,
                    settings = state.settings,
                    now = now,
                    onClick = { onOpenJob(job.id) },
                    partsToRedo = redoByJob[job.id] ?: 0
                )
            }
        }

        if (jobs.isEmpty() && state.loaded) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "Aucun plateau",
                    message = "Créez votre premier plateau pour suivre son temps, son coût et son résultat.",
                    actionLabel = "Nouveau plateau",
                    onAction = onNewJob
                )
            }
        }

        if (jobs.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total cumulé", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${formatMinutes(jobs.sumOf { it.effectiveMinutes(now) })} · " +
                                    formatMoney(
                                        jobs.sumOf { it.totalCost(state.settings, now) },
                                        state.settings.currency
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = onNewJob) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.width(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Plateau")
                        }
                    }
                }
            }
        }
    }
}
