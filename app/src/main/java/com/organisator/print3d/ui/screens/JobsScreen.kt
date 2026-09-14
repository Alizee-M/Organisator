@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.organisator.print3d.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PartStatus
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.Project
import com.organisator.print3d.data.effectiveMinutes
import com.organisator.print3d.data.totalCost
import com.organisator.print3d.ui.AppState
import com.organisator.print3d.ui.components.ChipSelector
import com.organisator.print3d.ui.components.EmptyState
import com.organisator.print3d.ui.components.JobRow
import com.organisator.print3d.ui.theme.AppIcons
import com.organisator.print3d.ui.theme.StatusPalette
import com.organisator.print3d.util.formatMinutes
import com.organisator.print3d.util.formatMoney
import com.organisator.print3d.util.parseColor

/** Façon dont la liste est ordonnée, et éventuellement regroupée. */
private enum class JobSort(val label: String) {
    PROJET("Par projet"),
    STATUT("Par statut"),
    RECENT("Récents")
}

/** Valeur sentinelle du filtre projet : -1 désigne les plateaux hors projet. */
private const val NO_PROJECT_ID = -1L

@Composable
fun JobsScreen(
    state: AppState,
    now: Long,
    onOpenJob: (Long) -> Unit,
    onNewJob: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    initialStatusFilter: JobStatus? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(initialStatusFilter) }
    var projectFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var sort by rememberSaveable { mutableStateOf(JobSort.PROJET) }
    val dark = isSystemInDarkTheme()
    val neutralChipColor = MaterialTheme.colorScheme.primary

    val projectsById = remember(state.projects) { state.projects.associateBy { it.id } }
    val redoByJob = remember(state.parts) {
        state.parts.filter { it.status == PartStatus.A_REFAIRE }
            .groupingBy { it.jobId }.eachCount()
    }

    val filtered = remember(state.jobs, query, statusFilter, projectFilter, sort, projectsById) {
        val q = query.trim().lowercase()
        state.jobs
            .filter { statusFilter == null || it.status == statusFilter }
            .filter { job ->
                when (projectFilter) {
                    null -> true
                    NO_PROJECT_ID -> job.projectId == null
                    else -> job.projectId == projectFilter
                }
            }
            .filter { job ->
                q.isBlank() ||
                    job.name.lowercase().contains(q) ||
                    job.fileName.lowercase().contains(q) ||
                    job.resinType.lowercase().contains(q) ||
                    job.notes.lowercase().contains(q) ||
                    projectsById[job.projectId]?.name?.lowercase()?.contains(q) == true
            }
            .sortedWith(sortComparator(sort, projectsById))
    }

    // En tri « par projet », la liste est découpée en sections précédées d'un en-tête.
    val sections = remember(filtered, sort, projectsById) {
        if (sort != JobSort.PROJET) listOf<Pair<Project?, List<PrintJob>>>(null to filtered)
        else filtered.groupBy { it.projectId }
            .toList()
            .sortedWith(
                // Les plateaux hors projet ferment la marche, le reste va par ordre alphabétique.
                compareBy<Pair<Long?, List<PrintJob>>> { it.first == null }
                    .thenBy { pair -> pair.first?.let { projectsById[it]?.name?.lowercase() } ?: "" }
            )
            .map { (projectId, jobs) -> projectId?.let { projectsById[it] } to jobs }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Plateaux", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Rechercher un plateau, un fichier…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            ChipSelector(
                options = listOf<JobStatus?>(null) + JobStatus.entries,
                selected = statusFilter,
                onSelect = { statusFilter = it },
                labelOf = { it?.label ?: "Tous les statuts" },
                colorOf = { status -> status?.let { StatusPalette.color(it, dark) } ?: neutralChipColor }
            )
        }

        if (state.projects.isNotEmpty()) {
            item {
                val options = buildList {
                    add(null)
                    state.projects.sortedBy { it.name.lowercase() }.forEach { add(it.id) }
                    if (state.jobs.any { it.projectId == null }) add(NO_PROJECT_ID)
                }
                ChipSelector(
                    options = options,
                    selected = projectFilter,
                    onSelect = { projectFilter = it },
                    labelOf = { id ->
                        when (id) {
                            null -> "Tous les projets"
                            NO_PROJECT_ID -> "Hors projet"
                            else -> id?.let { projectsById[it]?.name } ?: "Projet"
                        }
                    },
                    colorOf = { id ->
                        id?.let { projectsById[it] }
                            ?.let { parseColor(it.colorHex, neutralChipColor) }
                            ?: neutralChipColor
                    }
                )
            }
        }

        item {
            ChipSelector(
                options = JobSort.entries.toList(),
                selected = sort,
                onSelect = { sort = it },
                labelOf = { it.label }
            )
        }

        item {
            Text(
                text = "${filtered.size} plateau${if (filtered.size > 1) "x" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
        }

        sections.forEach { (project, jobs) ->
            if (sort == JobSort.PROJET) {
                item(key = "header-${project?.id ?: NO_PROJECT_ID}") {
                    ProjectSectionHeader(
                        project = project,
                        jobs = jobs,
                        settings = state.settings,
                        now = now
                    )
                }
            }
            items(jobs, key = { it.id }) { job ->
                JobRow(
                    job = job,
                    // Le nom du projet est déjà porté par l'en-tête de section.
                    projectName = if (sort == JobSort.PROJET) null else projectsById[job.projectId]?.name,
                    settings = state.settings,
                    now = now,
                    onClick = { onOpenJob(job.id) },
                    partsToRedo = redoByJob[job.id] ?: 0
                )
            }
        }

        if (filtered.isEmpty() && state.loaded) {
            item {
                EmptyState(
                    icon = AppIcons.Plateau,
                    title = if (state.jobs.isEmpty()) "Aucun plateau" else "Aucun résultat",
                    message = if (state.jobs.isEmpty())
                        "Ajoutez un plateau pour commencer le suivi."
                    else "Essayez un autre terme ou retirez les filtres.",
                    actionLabel = if (state.jobs.isEmpty()) "Nouveau plateau" else null,
                    onAction = if (state.jobs.isEmpty()) onNewJob else null
                )
            }
        }
    }
}

@Composable
private fun ProjectSectionHeader(
    project: Project?,
    jobs: List<PrintJob>,
    settings: com.organisator.print3d.data.Settings,
    now: Long
) {
    val color = project?.let { parseColor(it.colorHex, MaterialTheme.colorScheme.primary) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    val done = jobs.count { it.status == JobStatus.TERMINE }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = project?.name ?: "Hors projet",
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$done/${jobs.size} · ${formatMinutes(jobs.sumOf { it.effectiveMinutes(now) })} · " +
                formatMoney(jobs.sumOf { it.totalCost(settings, now) }, settings.currency),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun sortComparator(
    sort: JobSort,
    projectsById: Map<Long, Project>
): Comparator<PrintJob> = when (sort) {
    // Le regroupement se charge de l'ordre des projets ; ici on ordonne l'intérieur
    // d'un projet, du plus urgent au plus ancien.
    JobSort.PROJET -> compareBy<PrintJob> { statusOrder(it.status) }
        .thenByDescending { it.scheduledAt ?: it.updatedAt }

    JobSort.STATUT -> compareBy<PrintJob> { statusOrder(it.status) }
        .thenBy { job -> job.projectId?.let { projectsById[it]?.name?.lowercase() } ?: "\uFFFF" }
        .thenByDescending { it.scheduledAt ?: it.updatedAt }

    JobSort.RECENT -> compareByDescending { it.updatedAt }
}

/** Ordre d'affichage : ce qui demande une action passe en premier. */
private fun statusOrder(status: JobStatus): Int = when (status) {
    JobStatus.EN_COURS -> 0
    JobStatus.A_IMPRIMER -> 1
    JobStatus.A_REFAIRE -> 2
    JobStatus.A_FAIRE -> 3
    JobStatus.TERMINE -> 4
    JobStatus.ANNULE -> 5
}
