@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PartStatus
import com.organisator.print3d.ui.AppState
import com.organisator.print3d.ui.components.ChipSelector
import com.organisator.print3d.ui.components.EmptyState
import com.organisator.print3d.ui.components.JobRow
import com.organisator.print3d.ui.theme.StatusPalette
import androidx.compose.foundation.isSystemInDarkTheme

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
    val dark = isSystemInDarkTheme()

    val projectsById = remember(state.projects) { state.projects.associateBy { it.id } }
    val redoByJob = remember(state.parts) {
        state.parts.filter { it.status == PartStatus.A_REFAIRE }
            .groupingBy { it.jobId }.eachCount()
    }

    val filtered = remember(state.jobs, query, statusFilter, projectsById) {
        val q = query.trim().lowercase()
        state.jobs
            .filter { statusFilter == null || it.status == statusFilter }
            .filter { job ->
                q.isBlank() ||
                    job.name.lowercase().contains(q) ||
                    job.fileName.lowercase().contains(q) ||
                    job.material.lowercase().contains(q) ||
                    job.notes.lowercase().contains(q) ||
                    projectsById[job.projectId]?.name?.lowercase()?.contains(q) == true
            }
            .sortedWith(
                compareBy<com.organisator.print3d.data.PrintJob> { statusOrder(it.status) }
                    .thenByDescending { it.scheduledAt ?: it.updatedAt }
            )
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
            val options = listOf<JobStatus?>(null) + JobStatus.entries
            ChipSelector(
                options = options,
                selected = statusFilter,
                onSelect = { statusFilter = it },
                labelOf = { it?.label ?: "Tous" },
                colorOf = { status -> status?.let { StatusPalette.color(it, dark) } ?: MaterialTheme.colorScheme.primary }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Text(
                    text = "${filtered.size} plateau${if (filtered.size > 1) "x" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(filtered, key = { it.id }) { job ->
            JobRow(
                job = job,
                projectName = projectsById[job.projectId]?.name,
                settings = state.settings,
                now = now,
                onClick = { onOpenJob(job.id) },
                partsToRedo = redoByJob[job.id] ?: 0
            )
        }

        if (filtered.isEmpty() && state.loaded) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = if (state.jobs.isEmpty()) "Aucun plateau" else "Aucun résultat",
                    message = if (state.jobs.isEmpty())
                        "Ajoutez un plateau pour commencer le suivi."
                    else "Essayez un autre terme ou retirez le filtre.",
                    actionLabel = if (state.jobs.isEmpty()) "Nouveau plateau" else null,
                    onAction = if (state.jobs.isEmpty()) onNewJob else null
                )
            }
        }
    }
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
