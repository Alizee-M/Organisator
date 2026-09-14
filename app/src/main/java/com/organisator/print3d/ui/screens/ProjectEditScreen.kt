@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.organisator.print3d.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import com.organisator.print3d.ui.components.LocalImage
import com.organisator.print3d.data.Project
import com.organisator.print3d.data.ProjectStatus
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.ChipSelector
import com.organisator.print3d.ui.components.DateTimeField
import com.organisator.print3d.ui.components.FormTextField
import com.organisator.print3d.ui.components.SectionHeader
import com.organisator.print3d.ui.theme.StatusPalette
import com.organisator.print3d.util.parseColor

private val PROJECT_COLORS = listOf(
    "#6C7BFF", "#4F46E5", "#0EA5E9", "#10B981",
    "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6", "#64748B"
)

@Composable
fun ProjectEditScreen(
    existing: Project?,
    onSave: (Project) -> Unit,
    onDelete: (Project) -> Unit,
    onPickPhoto: (android.net.Uri, (String?) -> Unit) -> Unit,
    onDiscardPhoto: (String?) -> Unit,
    onBack: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var colorHex by remember(existing) { mutableStateOf(existing?.colorHex ?: PROJECT_COLORS.first()) }
    var status by remember(existing) { mutableStateOf(existing?.status ?: ProjectStatus.EN_COURS) }
    var deadline by remember(existing) { mutableStateOf(existing?.deadline) }
    var photoPath by remember(existing) { mutableStateOf(existing?.photoPath) }
    var importing by remember { mutableStateOf(false) }
    var importFailed by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        importFailed = false
        onPickPhoto(uri) { imported ->
            importing = false
            if (imported == null) {
                importFailed = true
            } else {
                // La photo remplacée n'est plus référencée nulle part.
                if (photoPath != existing?.photoPath) onDiscardPhoto(photoPath)
                photoPath = imported
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nouveau projet" else "Modifier le projet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (existing != null) {
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
                SectionHeader("Projet")
                Spacer(Modifier.height(10.dp))
                FormTextField("Nom", name, { name = it }, placeholder = "Ex. Boîtier Raspberry Pi")
                Spacer(Modifier.height(10.dp))
                FormTextField(
                    "Description", description, { description = it },
                    placeholder = "À quoi sert ce projet ?",
                    singleLine = false, minLines = 2
                )
            }

            AppCard {
                SectionHeader("Statut")
                Spacer(Modifier.height(10.dp))
                ChipSelector(
                    options = ProjectStatus.entries.toList(),
                    selected = status,
                    onSelect = { status = it },
                    labelOf = { it.label },
                    colorOf = { StatusPalette.color(it, dark) }
                )
            }

            AppCard {
                SectionHeader(
                    title = "Photo",
                    subtitle = if (photoPath == null) "Une image du projet, facultative"
                    else "Touchez l'image pour la remplacer"
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoPath != null) {
                        LocalImage(
                            path = photoPath,
                            contentDescription = "Photo du projet",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = if (importing) "Import en cours…" else "Ajouter une photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (importFailed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Cette image n'a pas pu être lue. Essayez-en une autre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (photoPath != null) {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        TextButton(onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) { Text("Remplacer") }
                        TextButton(onClick = {
                            if (photoPath != existing?.photoPath) onDiscardPhoto(photoPath)
                            photoPath = null
                        }) { Text("Retirer") }
                    }
                }
            }

            AppCard {
                SectionHeader("Couleur", subtitle = "Pour repérer le projet d'un coup d'œil")
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PROJECT_COLORS.forEach { hex ->
                        val color = parseColor(hex, MaterialTheme.colorScheme.primary)
                        val selected = hex == colorHex
                        Box(
                            modifier = Modifier
                                .size(if (selected) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }

            AppCard {
                SectionHeader("Échéance")
                Spacer(Modifier.height(10.dp))
                DateTimeField(label = "Date limite", value = deadline, onChange = { deadline = it })
            }

            Button(
                onClick = {
                    onSave(
                        (existing ?: Project(name = "")).copy(
                            name = name.trim().ifBlank { "Projet sans nom" },
                            description = description.trim(),
                            colorHex = colorHex,
                            status = status,
                            deadline = deadline,
                            photoPath = photoPath
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(if (existing == null) "Créer le projet" else "Enregistrer") }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Supprimer ce projet ?") },
            text = { Text("Les plateaux liés seront conservés, mais détachés du projet.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete(existing)
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Annuler") } }
        )
    }
}
