@file:OptIn(ExperimentalMaterial3Api::class)

package com.organisator.print3d.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organisator.print3d.data.MaintenanceEntry
import com.organisator.print3d.data.MaintenanceKind
import com.organisator.print3d.data.PRINTERS
import com.organisator.print3d.ui.AppState
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.Chip
import com.organisator.print3d.ui.components.ChipSelector
import com.organisator.print3d.ui.components.DateTimeField
import com.organisator.print3d.ui.components.DetailRow
import com.organisator.print3d.ui.components.DropdownField
import com.organisator.print3d.ui.components.EmptyState
import com.organisator.print3d.ui.components.FormTextField
import com.organisator.print3d.ui.components.SectionHeader
import com.organisator.print3d.util.formatDate
import com.organisator.print3d.util.formatRelative
import java.util.Locale

/** Journal d'entretien : ce qui a été fait, quand, et à combien de couches. */
@Composable
fun MaintenanceScreen(
    state: AppState,
    now: Long,
    onSave: (MaintenanceEntry) -> Unit,
    onDelete: (MaintenanceEntry) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var printerFilter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<MaintenanceEntry?>(null) }
    var creating by remember { mutableStateOf(false) }

    val entries = remember(state.maintenance, printerFilter) {
        state.maintenance.filter { printerFilter == null || it.printer == printerFilter }
    }
    val lastByPrinter = remember(state.maintenance) {
        state.maintenance.groupBy { it.printer }.mapValues { (_, list) -> list.maxByOrNull { it.date } }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Entretien", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Films, écrans, nettoyages et consommables, machine par machine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Button(
                onClick = { creating = true },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Noter un entretien") }
        }

        item {
            SectionHeader("Parc", subtitle = "Dernière intervention par machine")
        }

        items(PRINTERS, key = { "printer-$it" }) { printer ->
            val last = lastByPrinter[printer]
            AppCard(contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = printer,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (last != null) {
                        Chip(
                            label = last.kind.label,
                            color = MaterialTheme.colorScheme.primary,
                            compact = true
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (last == null) "Aucune intervention notée"
                    else "${formatDate(last.date)} · ${formatRelative(last.date, now)} · " +
                        "${formatLayers(last.layerCount)} couches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader("Historique")
        }

        item {
            ChipSelector(
                options = listOf<String?>(null) + PRINTERS,
                selected = printerFilter,
                onSelect = { printerFilter = it },
                labelOf = { it ?: "Toutes" }
            )
        }

        items(entries, key = { it.id }) { entry ->
            AppCard(
                modifier = Modifier.clickable { editing = entry },
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip(
                        label = entry.kind.label,
                        color = MaterialTheme.colorScheme.primary,
                        compact = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.printer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDate(entry.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.layerCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${formatLayers(entry.layerCount)} couches",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (entry.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (entries.isEmpty() && state.loaded) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Build,
                    title = "Rien de noté",
                    message = "Consignez les changements de film, d'écran et de résine : " +
                        "le compteur de couches dira quand refaire le prochain.",
                    actionLabel = "Noter un entretien",
                    onAction = { creating = true }
                )
            }
        }
    }

    if (creating) {
        MaintenanceDialog(
            existing = null,
            onDismiss = { creating = false },
            onConfirm = {
                onSave(it)
                creating = false
            },
            onDelete = null
        )
    }

    editing?.let { entry ->
        MaintenanceDialog(
            existing = entry,
            onDismiss = { editing = null },
            onConfirm = {
                onSave(it)
                editing = null
            },
            onDelete = {
                onDelete(entry)
                editing = null
            }
        )
    }
}

@Composable
private fun MaintenanceDialog(
    existing: MaintenanceEntry?,
    onDismiss: () -> Unit,
    onConfirm: (MaintenanceEntry) -> Unit,
    onDelete: (() -> Unit)?
) {
    var printer by remember(existing) { mutableStateOf(existing?.printer ?: PRINTERS.first()) }
    var kind by remember(existing) { mutableStateOf(existing?.kind ?: MaintenanceKind.FILM_FEP) }
    var date by remember(existing) { mutableStateOf(existing?.date ?: System.currentTimeMillis()) }
    var layers by remember(existing) {
        mutableStateOf(existing?.layerCount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nouvel entretien" else "Modifier l'entretien") },
        text = {
            Column {
                DropdownField(
                    label = "Imprimante",
                    value = printer,
                    options = PRINTERS,
                    allowCustom = false,
                    onSelect = { printer = it }
                )
                Spacer(Modifier.height(10.dp))
                ChipSelector(
                    options = MaintenanceKind.entries.toList(),
                    selected = kind,
                    onSelect = { kind = it },
                    labelOf = { it.label }
                )
                Spacer(Modifier.height(10.dp))
                DateTimeField(
                    label = "Date",
                    value = date,
                    onChange = { date = it ?: System.currentTimeMillis() }
                )
                Spacer(Modifier.height(10.dp))
                FormTextField(
                    label = "Compteur de couches",
                    value = layers,
                    onValueChange = { layers = it.filterDigits() },
                    placeholder = "Ex. 45000",
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(10.dp))
                FormTextField(
                    label = "Remarque",
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "Référence du film, observation…",
                    singleLine = false,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    (existing ?: MaintenanceEntry(printer = printer)).copy(
                        printer = printer,
                        kind = kind,
                        date = date,
                        layerCount = layers.toIntOrZero(),
                        notes = notes.trim()
                    )
                )
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}

/** « 45 000 » se lit mieux que « 45000 » quand on compare deux relevés. */
private fun formatLayers(count: Int): String =
    String.format(Locale.FRANCE, "%,d", count).replace(' ', ' ')
