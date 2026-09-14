package com.organisator.print3d.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organisator.print3d.ui.theme.AppIcons
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.Project
import com.organisator.print3d.data.Settings
import com.organisator.print3d.data.effectiveMinutes
import com.organisator.print3d.data.totalCost
import com.organisator.print3d.ui.AppState
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.EmptyState
import com.organisator.print3d.ui.components.ProgressBar
import com.organisator.print3d.ui.components.StatusChip
import com.organisator.print3d.util.formatDate
import com.organisator.print3d.util.formatMinutes
import com.organisator.print3d.util.formatMoney
import com.organisator.print3d.util.parseColor

@Composable
fun ProjectsScreen(
    state: AppState,
    now: Long,
    onOpenProject: (Long) -> Unit,
    onNewProject: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val jobsByProject = remember(state.jobs) { state.jobs.groupBy { it.projectId } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Projets", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Regroupez vos plateaux et suivez l'avancement global.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.projects, key = { it.id }) { project ->
            ProjectCard(
                project = project,
                jobs = jobsByProject[project.id].orEmpty(),
                settings = state.settings,
                now = now,
                onClick = { onOpenProject(project.id) }
            )
        }

        val orphans = jobsByProject[null].orEmpty()
        if (orphans.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                AppCard {
                    Text("Hors projet", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${orphans.size} plateau${if (orphans.size > 1) "x" else ""} sans projet · " +
                            formatMinutes(orphans.sumOf { it.effectiveMinutes(now) }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.projects.isEmpty() && state.loaded) {
            item {
                EmptyState(
                    icon = AppIcons.Projet,
                    title = "Aucun projet",
                    message = "Créez un projet pour regrouper plusieurs plateaux et suivre son coût total.",
                    actionLabel = "Nouveau projet",
                    onAction = onNewProject
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    jobs: List<PrintJob>,
    settings: Settings,
    now: Long,
    onClick: () -> Unit
) {
    val color = parseColor(project.colorHex, MaterialTheme.colorScheme.primary)
    val done = jobs.count { it.status == JobStatus.TERMINE }
    val progress = if (jobs.isEmpty()) 0f else done.toFloat() / jobs.size

    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (project.description.isNotBlank()) {
                    Text(
                        project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(project.status, compact = true)
        }

        Spacer(Modifier.height(12.dp))
        ProgressBar(progress = progress, color = color)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Meta("$done/${jobs.size} plateaux")
            Meta(formatMinutes(jobs.sumOf { it.effectiveMinutes(now) }))
            Meta(formatMoney(jobs.sumOf { it.totalCost(settings, now) }, settings.currency))
        }
        project.deadline?.let {
            Spacer(Modifier.height(6.dp))
            Meta("Échéance ${formatDate(it)}")
        }
    }
}

@Composable
private fun Meta(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}
