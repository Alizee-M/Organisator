@file:OptIn(ExperimentalMaterial3Api::class)

package com.organisator.print3d.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.Project
import com.organisator.print3d.data.Settings
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.ChipSelector
import com.organisator.print3d.ui.components.DateTimeField
import com.organisator.print3d.ui.components.DropdownField
import com.organisator.print3d.ui.components.FormTextField
import com.organisator.print3d.ui.components.SectionHeader
import com.organisator.print3d.ui.components.TwoColumns
import com.organisator.print3d.ui.theme.StatusPalette

private val RESIN_TYPES = listOf(
    "Résine standard", "ABS-like", "Water-washable", "Tough",
    "Flexible", "Transparente", "Castable", "Dentaire", "Haute température"
)
private val LAYER_HEIGHTS = listOf("20", "25", "30", "35", "40", "50", "60", "80", "100")
private const val NO_PROJECT = "Aucun projet"

@Composable
fun JobEditScreen(
    existing: PrintJob?,
    projects: List<Project>,
    settings: Settings,
    preselectedProjectId: Long?,
    onSave: (PrintJob) -> Unit,
    onDelete: (PrintJob) -> Unit,
    onBack: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val base = existing

    var name by remember(base) { mutableStateOf(base?.name ?: "") }
    var projectId by remember(base) { mutableStateOf(base?.projectId ?: preselectedProjectId) }
    var fileName by remember(base) { mutableStateOf(base?.fileName ?: "") }
    var printer by remember(base) { mutableStateOf(base?.printer ?: settings.defaultPrinter) }
    var resinType by remember(base) { mutableStateOf(base?.resinType ?: settings.defaultResinType) }
    var resinColor by remember(base) { mutableStateOf(base?.resinColor ?: "") }
    var status by remember(base) { mutableStateOf(base?.status ?: JobStatus.A_FAIRE) }
    var scale by remember(base) { mutableStateOf((base?.scalePercent ?: 100).toString()) }
    var quantity by remember(base) { mutableStateOf((base?.quantity ?: 1).toString()) }
    var estHours by remember(base) { mutableStateOf(((base?.estimatedMinutes ?: 0) / 60).toString()) }
    var estMinutes by remember(base) { mutableStateOf(((base?.estimatedMinutes ?: 0) % 60).toString()) }
    var realHours by remember(base) { mutableStateOf(((base?.actualMinutes ?: 0) / 60).toString()) }
    var realMinutes by remember(base) { mutableStateOf(((base?.actualMinutes ?: 0) % 60).toString()) }
    var resinMl by remember(base) { mutableStateOf(base?.resinMl?.takeIf { it > 0 }?.trimNumber() ?: "") }
    var layerHeight by remember(base) { mutableStateOf((base?.layerHeightMicrons ?: 50).toString()) }
    var pricePerLitre by remember(base) {
        mutableStateOf(
            (base?.resinPricePerLitre?.takeIf { it > 0 } ?: settings.defaultResinPricePerLitre).trimNumber()
        )
    }
    var extraCost by remember(base) { mutableStateOf(base?.extraCost?.takeIf { it > 0 }?.trimNumber() ?: "") }
    var scheduledAt by remember(base) { mutableStateOf(base?.scheduledAt) }
    var reminderEnabled by remember(base) { mutableStateOf(base?.reminderEnabled ?: false) }
    var reminderAt by remember(base) { mutableStateOf(base?.reminderAt) }
    var notes by remember(base) { mutableStateOf(base?.notes ?: "") }
    var showDelete by remember { mutableStateOf(false) }

    val projectOptions = remember(projects) { listOf(NO_PROJECT) + projects.map { it.name } }
    val projectLabel = projects.firstOrNull { it.id == projectId }?.name ?: NO_PROJECT

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (base == null) "Nouveau plateau" else "Modifier") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (base != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
            AppCard {
                SectionHeader("Identification")
                Spacer(Modifier.height(10.dp))
                FormTextField("Nom du plateau", name, { name = it }, placeholder = "Ex. Boîtier v2 — lot de 4")
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Projet",
                    value = projectLabel,
                    options = projectOptions,
                    allowCustom = false,
                    onSelect = { selected ->
                        projectId = if (selected == NO_PROJECT) null
                        else projects.firstOrNull { it.name == selected }?.id
                    }
                )
                Spacer(Modifier.height(10.dp))
                FormTextField("Fichier imprimé", fileName, { fileName = it }, placeholder = "boitier_v2.3mf")
                Spacer(Modifier.height(10.dp))
                FormTextField("Imprimante", printer, { printer = it }, placeholder = "Ex. A1 mini")
            }

            AppCard {
                SectionHeader("Statut", subtitle = "Où en est ce plateau")
                Spacer(Modifier.height(10.dp))
                ChipSelector(
                    options = JobStatus.entries.toList(),
                    selected = status,
                    onSelect = { status = it },
                    labelOf = { it.label },
                    colorOf = { StatusPalette.color(it, dark) }
                )
            }

            AppCard {
                SectionHeader("Impression")
                Spacer(Modifier.height(10.dp))
                TwoColumns(
                    left = {
                        DropdownField(
                            label = "Résine",
                            value = resinType,
                            options = RESIN_TYPES,
                            onSelect = { resinType = it }
                        )
                    },
                    right = {
                        FormTextField("Couleur", resinColor, { resinColor = it }, placeholder = "Gris mat")
                    }
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Hauteur de couche (µm)",
                    value = layerHeight,
                    options = LAYER_HEIGHTS,
                    onSelect = { layerHeight = it.filterDigits() }
                )
                Spacer(Modifier.height(10.dp))
                TwoColumns(
                    left = {
                        FormTextField(
                            "Échelle", scale, { scale = it.filterDigits() },
                            keyboardType = KeyboardType.Number, suffix = "%"
                        )
                    },
                    right = {
                        FormTextField(
                            "Exemplaires", quantity, { quantity = it.filterDigits() },
                            keyboardType = KeyboardType.Number
                        )
                    }
                )
                Spacer(Modifier.height(14.dp))
                Text("Temps estimé", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                TwoColumns(
                    left = {
                        FormTextField("Heures", estHours, { estHours = it.filterDigits() },
                            keyboardType = KeyboardType.Number, suffix = "h")
                    },
                    right = {
                        FormTextField("Minutes", estMinutes, { estMinutes = it.filterDigits() },
                            keyboardType = KeyboardType.Number, suffix = "min")
                    }
                )
                Spacer(Modifier.height(14.dp))
                Text("Temps réel", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                TwoColumns(
                    left = {
                        FormTextField("Heures", realHours, { realHours = it.filterDigits() },
                            keyboardType = KeyboardType.Number, suffix = "h")
                    },
                    right = {
                        FormTextField("Minutes", realMinutes, { realMinutes = it.filterDigits() },
                            keyboardType = KeyboardType.Number, suffix = "min")
                    }
                )
            }

            AppCard {
                SectionHeader("Coûts", subtitle = "Énergie et consommables viennent des réglages")
                Spacer(Modifier.height(10.dp))
                TwoColumns(
                    left = {
                        FormTextField("Résine", resinMl, { resinMl = it.filterDecimal() },
                            keyboardType = KeyboardType.Decimal, suffix = "mL")
                    },
                    right = {
                        FormTextField("Prix résine", pricePerLitre, { pricePerLitre = it.filterDecimal() },
                            keyboardType = KeyboardType.Decimal, suffix = "${settings.currency}/L")
                    }
                )
                Spacer(Modifier.height(10.dp))
                FormTextField(
                    "Coûts annexes", extraCost, { extraCost = it.filterDecimal() },
                    placeholder = "Apprêt, peinture, ponçage…",
                    keyboardType = KeyboardType.Decimal, suffix = settings.currency
                )
            }

            AppCard {
                SectionHeader("Planification", subtitle = "Et rappel sur le téléphone")
                Spacer(Modifier.height(10.dp))
                DateTimeField(
                    label = "Prévu le",
                    value = scheduledAt,
                    onChange = { picked ->
                        scheduledAt = picked
                        if (picked != null && reminderAt == null) reminderAt = picked
                    }
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Me le rappeler", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Une notification à l'heure choisie",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = {
                            reminderEnabled = it
                            if (it && reminderAt == null) reminderAt = scheduledAt
                        }
                    )
                }
                if (reminderEnabled) {
                    Spacer(Modifier.height(10.dp))
                    DateTimeField(
                        label = "Rappel",
                        value = reminderAt,
                        onChange = { reminderAt = it }
                    )
                }
            }

            AppCard {
                SectionHeader("Mémo")
                Spacer(Modifier.height(10.dp))
                FormTextField(
                    label = "Notes",
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "Exposition, orientation, supports, remarques…",
                    singleLine = false,
                    minLines = 3
                )
            }

            Button(
                onClick = {
                    val est = estHours.toIntOrZero() * 60 + estMinutes.toIntOrZero()
                    val real = realHours.toIntOrZero() * 60 + realMinutes.toIntOrZero()
                    val job = (base ?: PrintJob(name = "")).copy(
                        name = name.trim().ifBlank { "Plateau sans nom" },
                        projectId = projectId,
                        fileName = fileName.trim(),
                        printer = printer.trim(),
                        resinType = resinType.trim(),
                        resinColor = resinColor.trim(),
                        status = status,
                        scalePercent = scale.toIntOrZero().let { if (it <= 0) 100 else it }.coerceAtMost(1000),
                        quantity = quantity.toIntOrZero().coerceAtLeast(1),
                        estimatedMinutes = est,
                        actualMinutes = real,
                        resinMl = resinMl.toDoubleOrZero(),
                        resinPricePerLitre = pricePerLitre.toDoubleOrZero(),
                        layerHeightMicrons = layerHeight.toIntOrZero().let { if (it <= 0) 50 else it },
                        extraCost = extraCost.toDoubleOrZero(),
                        scheduledAt = scheduledAt,
                        reminderEnabled = reminderEnabled && reminderAt != null,
                        reminderAt = reminderAt,
                        notes = notes.trim(),
                        startedAt = if (status == JobStatus.EN_COURS && base?.startedAt == null)
                            System.currentTimeMillis() else base?.startedAt
                    )
                    onSave(job)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(if (base == null) "Créer le plateau" else "Enregistrer") }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDelete && base != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Supprimer ce plateau ?") },
            text = { Text("« ${base.name} » et ses pièces seront définitivement effacés.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete(base)
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Annuler") }
            }
        )
    }
}

internal fun String.filterDigits(): String = filter { it.isDigit() }.take(6)

internal fun String.filterDecimal(): String =
    replace(',', '.').filter { it.isDigit() || it == '.' }.take(9)

internal fun String.toIntOrZero(): Int = trim().toIntOrNull() ?: 0

internal fun String.toDoubleOrZero(): Double = trim().replace(',', '.').toDoubleOrNull() ?: 0.0

/** Affiche 22.0 comme « 22 » et 22.5 comme « 22.5 » dans les champs. */
internal fun Double.trimNumber(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
