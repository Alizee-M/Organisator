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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.Project
import com.organisator.print3d.data.Settings
import com.organisator.print3d.data.effectiveMinutes
import com.organisator.print3d.data.totalCost
import com.organisator.print3d.ui.AppState
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.EmptyState
import com.organisator.print3d.ui.components.LocalImage
import com.organisator.print3d.ui.components.ProgressBar
import com.organisator.print3d.ui.components.StatusChip
import com.organisator.print3d.ui.theme.AppIcons
import com.organisator.print3d.util.formatMinutes
import com.organisator.print3d.util.formatMoney
import com.organisator.print3d.util.parseColor

private val TILE_SIZE = 78.dp
private val TILE_SHAPE = RoundedCornerShape(22.dp)

/**
 * Écran d'accueil : les projets tiennent lieu de menu. Chaque entrée montre sa
 * photo à gauche et son nom à droite, et mène au détail du projet.
 */
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 2.dp)) {
                Text("Projets", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Touchez un projet pour voir ses plateaux.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.projects, key = { it.id }) { project ->
            ProjectTile(
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
                AppCard(contentPadding = PaddingValues(14.dp)) {
                    Text("Hors projet", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${orphans.size} plateau${if (orphans.size > 1) "x" else ""} · " +
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
                    message = "Créez un projet pour regrouper vos plateaux et suivre son coût total.",
                    actionLabel = "Nouveau projet",
                    onAction = onNewProject
                )
            }
        }
    }
}

@Composable
private fun ProjectTile(
    project: Project,
    jobs: List<PrintJob>,
    settings: Settings,
    now: Long,
    onClick: () -> Unit
) {
    val color = parseColor(project.colorHex, MaterialTheme.colorScheme.primary)
    val done = jobs.count { it.status == JobStatus.TERMINE }
    val progress = if (jobs.isEmpty()) 0f else done.toFloat() / jobs.size

    AppCard(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProjectThumbnail(project = project, color = color)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                StatusChip(project.status, compact = true)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$done/${jobs.size} plateaux · " +
                        formatMinutes(jobs.sumOf { it.effectiveMinutes(now) }) + " · " +
                        formatMoney(jobs.sumOf { it.totalCost(settings, now) }, settings.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                ProgressBar(progress = progress, color = color, height = 5.dp)
            }

            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Photo du projet, ou son initiale sur un aplat de sa couleur. */
@Composable
private fun ProjectThumbnail(
    project: Project,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .size(TILE_SIZE)
            .clip(TILE_SHAPE)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        if (project.photoPath != null) {
            LocalImage(
                path = project.photoPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = project.name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
