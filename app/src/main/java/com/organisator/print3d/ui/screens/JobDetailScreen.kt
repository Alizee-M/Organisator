@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.organisator.print3d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PartStatus
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.PrintPart
import com.organisator.print3d.data.Project
import com.organisator.print3d.data.Settings
import com.organisator.print3d.data.costBreakdown
import com.organisator.print3d.data.effectiveMinutes
import com.organisator.print3d.data.elapsedMinutes
import com.organisator.print3d.data.progress
import com.organisator.print3d.data.remainingMinutes
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.Chip
import com.organisator.print3d.ui.components.Divider
import com.organisator.print3d.ui.components.DropdownField
import com.organisator.print3d.ui.components.DetailRow
import com.organisator.print3d.ui.components.FormTextField
import com.organisator.print3d.ui.components.ProgressBar
import com.organisator.print3d.ui.components.SectionHeader
import com.organisator.print3d.ui.components.StatusChip
import com.organisator.print3d.ui.theme.StatusPalette
import com.organisator.print3d.ui.theme.isDarkTheme
import com.organisator.print3d.ui.theme.statusColor
import com.organisator.print3d.util.formatDateTime
import com.organisator.print3d.util.formatMillilitres
import com.organisator.print3d.util.formatMinutes
import com.organisator.print3d.util.formatMoney
import com.organisator.print3d.util.formatRelative

@Composable
fun JobDetailScreen(
    job: PrintJob,
    project: Project?,
    parts: List<PrintPart>,
    settings: Settings,
    now: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStatus: (JobStatus) -> Unit,
    onOutcome: (Boolean) -> Unit,
    projects: List<Project>,
    onProjectChange: (Long?) -> Unit,
    onAddParts: (String) -> Unit,
    onTogglePart: (PrintPart) -> Unit,
    onDeletePart: (PrintPart) -> Unit,
    onReprint: () -> Unit
) {
    val dark = isDarkTheme()
    var newParts by remember(job.id) { mutableStateOf("") }
    val cost = job.costBreakdown(settings, now)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plateau", style = MaterialTheme.typography.titleMedium) },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(job.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(job.status)
                    if (project != null) {
                        Chip(
                            label = project.name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            compact = true
                        )
                    }
                }
            }

            val progress = job.progress(now)
            if (job.status == JobStatus.EN_COURS) {
                AppCard {
                    Text("Impression en cours", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    if (progress != null) {
                        ProgressBar(progress = progress, color = statusColor(JobStatus.EN_COURS), height = 8.dp)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = buildString {
                            append(formatMinutes(job.elapsedMinutes(now)))
                            append(" écoulées")
                            job.remainingMinutes(now)?.let {
                                append(" · reste ")
                                append(if (it == 0) "quelques instants" else formatMinutes(it))
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppCard {
                SectionHeader(
                    title = "Projet",
                    subtitle = "Modifiable même pendant l'impression"
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Rattaché à",
                    value = project?.name ?: NO_PROJECT,
                    options = listOf(NO_PROJECT) + projects.map { it.name },
                    allowCustom = false,
                    onSelect = { selected ->
                        onProjectChange(
                            if (selected == NO_PROJECT) null
                            else projects.firstOrNull { it.name == selected }?.id
                        )
                    }
                )
            }

            ActionBar(
                job = job,
                onStatus = onStatus,
                onOutcome = onOutcome,
                onReprint = onReprint
            )

            AppCard {
                val toRedo = parts.count { it.status == PartStatus.A_REFAIRE }
                SectionHeader(
                    title = "Pièces du plateau",
                    subtitle = if (parts.isEmpty()) "Séparez les noms par des espaces"
                    else "Touchez une pièce ratée pour la passer en rouge"
                )
                if (parts.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        parts.forEach { part ->
                            PartChip(
                                part = part,
                                onClick = { onTogglePart(part) },
                                onLongClick = { onDeletePart(part) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (toRedo == 0) "Appui long pour retirer une pièce."
                        else "$toRedo pièce${if (toRedo > 1) "s" else ""} à refaire · appui long pour en retirer une.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (toRedo == 0) MaterialTheme.colorScheme.onSurfaceVariant
                        else statusColor(JobStatus.A_REFAIRE)
                    )
                }
                Spacer(Modifier.height(12.dp))
                FormTextField(
                    label = "Ajouter des pièces",
                    value = newParts,
                    onValueChange = { newParts = it },
                    placeholder = "tête bras jambes"
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            onAddParts(newParts)
                            newParts = ""
                        },
                        enabled = newParts.isNotBlank()
                    ) { Text("Ajouter") }
                }
            }

            AppCard {
                SectionHeader("Résultat du plateau")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutcomeButton(
                        label = "Tout est bon",
                        selected = job.plateFullyOk == true,
                        color = StatusPalette.color(JobStatus.TERMINE, dark),
                        onClick = { onOutcome(true) },
                        modifier = Modifier.weight(1f)
                    )
                    OutcomeButton(
                        label = "Pièces à refaire",
                        selected = job.plateFullyOk == false,
                        color = StatusPalette.color(JobStatus.A_REFAIRE, dark),
                        onClick = { onOutcome(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (job.plateFullyOk == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pas encore évalué.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppCard {
                SectionHeader("Temps")
                Spacer(Modifier.height(6.dp))
                DetailRow("Estimé", formatMinutes(job.estimatedMinutes))
                DetailRow("Réel", formatMinutes(job.actualMinutes))
                DetailRow("Retenu pour les stats", formatMinutes(job.effectiveMinutes(now)))
                job.startedAt?.let { DetailRow("Démarré", formatDateTime(it)) }
                job.finishedAt?.let { DetailRow("Terminé", formatDateTime(it)) }
            }

            AppCard {
                SectionHeader("Coût", subtitle = "Détail par poste")
                Spacer(Modifier.height(6.dp))
                DetailRow("Résine (${formatMillilitres(job.resinMl)})", formatMoney(cost.resin, settings.currency))
                if (settings.includeEnergyInCost) {
                    DetailRow("Électricité", formatMoney(cost.energy, settings.currency))
                }
                if (settings.hourlyMachineRate > 0) {
                    DetailRow("Machine", formatMoney(cost.machine, settings.currency))
                }
                if (settings.consumablesPerPrint > 0) {
                    DetailRow("Lavage et durcissement", formatMoney(cost.consumables, settings.currency))
                }
                if (job.extraCost > 0) {
                    DetailRow("Annexes", formatMoney(cost.extra, settings.currency))
                }
                Spacer(Modifier.height(6.dp))
                Divider()
                Spacer(Modifier.height(6.dp))
                DetailRow(
                    "Total",
                    formatMoney(cost.total, settings.currency),
                    valueColor = MaterialTheme.colorScheme.primary
                )
                if (job.quantity > 1) {
                    DetailRow("Par exemplaire", formatMoney(cost.total / job.quantity, settings.currency))
                }
            }

            AppCard {
                SectionHeader("Détails")
                Spacer(Modifier.height(6.dp))
                if (job.fileName.isNotBlank()) DetailRow("Fichier", job.fileName)
                if (job.printer.isNotBlank()) DetailRow("Imprimante", job.printer)
                DetailRow("Résine", listOfNotNull(
                    job.resinType.takeIf { it.isNotBlank() },
                    job.resinColor.takeIf { it.isNotBlank() }
                ).joinToString(" · ").ifBlank { "—" })
                DetailRow("Échelle", "${job.scalePercent} %")
                DetailRow("Exemplaires", job.quantity.toString())
                DetailRow("Hauteur de couche", "${job.layerHeightMicrons} µm")
                DetailRow("Volume de résine", formatMillilitres(job.resinMl))
            }

            if (job.scheduledAt != null || (job.reminderEnabled && job.reminderAt != null)) {
                AppCard {
                    SectionHeader("Planification")
                    Spacer(Modifier.height(6.dp))
                    job.scheduledAt?.let {
                        DetailRow("Prévu le", "${formatDateTime(it)} (${formatRelative(it, now)})")
                    }
                    job.reminderAt?.takeIf { job.reminderEnabled }?.let {
                        DetailRow(
                            "Rappel",
                            "${formatDateTime(it)} (${formatRelative(it, now)})",
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (job.notes.isNotBlank()) {
                AppCard {
                    SectionHeader("Mémo")
                    Spacer(Modifier.height(8.dp))
                    Text(job.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

}

@Composable
private fun ActionBar(
    job: PrintJob,
    onStatus: (JobStatus) -> Unit,
    onOutcome: (Boolean) -> Unit,
    onReprint: () -> Unit
) {
    when (job.status) {
        JobStatus.A_FAIRE, JobStatus.A_IMPRIMER -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { onStatus(JobStatus.EN_COURS) },
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Lancer l'impression") }
            if (job.status == JobStatus.A_FAIRE) {
                OutlinedButton(
                    onClick = { onStatus(JobStatus.A_IMPRIMER) },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Mettre en file") }
            }
        }

        JobStatus.EN_COURS -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { onOutcome(true) },
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Plateau réussi") }
            OutlinedButton(
                onClick = { onOutcome(false) },
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Raté") }
        }

        JobStatus.TERMINE, JobStatus.A_REFAIRE, JobStatus.ANNULE -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onReprint,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Réimprimer") }
            OutlinedButton(
                onClick = { onStatus(JobStatus.EN_COURS) },
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Relancer") }
        }
    }
}

@Composable
private fun OutcomeButton(
    label: String,
    selected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) color.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Une pièce se présente comme un bouton : neutre tant que tout va bien, rouge
 * dès qu'elle est marquée à refaire.
 */
@Composable
private fun PartChip(
    part: PrintPart,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val failed = part.status == PartStatus.A_REFAIRE
    val accent = statusColor(JobStatus.A_REFAIRE)
    val background = if (failed) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (failed) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    val content = if (failed) accent else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(999.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Text(
            text = if (part.quantity > 1) "${part.name} ×${part.quantity}" else part.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (failed) FontWeight.SemiBold else FontWeight.Medium,
            color = content
        )
    }
}
