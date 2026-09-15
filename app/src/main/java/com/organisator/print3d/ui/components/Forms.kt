@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.organisator.print3d.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.organisator.print3d.util.formatDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowCustom: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (allowCustom) onSelect(it) },
            readOnly = !allowCustom,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            },
            shape = RoundedCornerShape(14.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                // Un champ en lecture seule ne s'ouvre au toucher qu'avec l'ancrage
                // « non éditable » : sans lui, la liste reste fermée.
                .menuAnchor(
                    if (allowCustom) androidx.compose.material3.MenuAnchorType.PrimaryEditable
                    else androidx.compose.material3.MenuAnchorType.PrimaryNotEditable,
                    true
                )
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Champ date + heure : ouvre le calendrier puis l'horloge, et permet d'effacer. */
@Composable
fun DateTimeField(
    label: String,
    value: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value?.let { formatDateTime(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(label) },
                placeholder = { Text("Non planifié") },
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
            // Le champ est désactivé pour rester en lecture seule : la zone cliquable
            // est posée par-dessus pour ouvrir le sélecteur.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPicker = true }
            )
        }
        if (value != null) {
            TextButton(onClick = { onChange(null) }) { Text("Effacer") }
        }
    }

    if (showPicker) {
        DateTimePickerDialog(
            initial = value,
            onDismiss = { showPicker = false },
            onConfirm = {
                onChange(it)
                showPicker = false
            }
        )
    }
}

@Composable
fun DateTimePickerDialog(
    initial: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val base = initial?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
    } ?: LocalDateTime.now().plusHours(1).withMinute(0)

    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    if (pickedDate == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = base.toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = dateState.selectedDateMillis
                        pickedDate = if (millis != null) {
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        } else base.toLocalDate()
                    }
                ) { Text("Suivant") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = base.hour,
            initialMinute = base.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = pickedDate ?: return@TextButton
                        val dateTime = date.atTime(timeState.hour, timeState.minute)
                        onConfirm(
                            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                    }
                ) { Text("Valider") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
            title = { Text("Heure") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timeState)
                }
            }
        )
    }
}

/** Rangée de puces de sélection, repliée automatiquement sur plusieurs lignes. */
@Composable
fun <T> ChipSelector(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier,
    colorOf: ((T) -> Color)? = null
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val accent = colorOf?.invoke(option)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = { Text(labelOf(option)) },
                shape = RoundedCornerShape(999.dp),
                colors = if (accent != null) {
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.16f),
                        selectedLabelColor = accent
                    )
                } else FilterChipDefaults.filterChipColors(),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = (accent ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
fun FormSpacer(height: Int = 12) {
    Spacer(Modifier.height(height.dp))
}

@Composable
fun TwoColumns(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) { left() }
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) { right() }
    }
}
