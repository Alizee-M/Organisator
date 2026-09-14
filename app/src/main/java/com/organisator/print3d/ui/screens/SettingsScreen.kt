@file:OptIn(ExperimentalMaterial3Api::class)

package com.organisator.print3d.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.organisator.print3d.data.Settings
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.FormTextField
import com.organisator.print3d.ui.components.SectionHeader
import com.organisator.print3d.ui.components.TwoColumns

@Composable
fun SettingsScreen(
    settings: Settings,
    appVersion: String,
    exactAlarmsAllowed: Boolean,
    notificationsAllowed: Boolean,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSave: (Settings) -> Unit,
    onBack: () -> Unit
) {
    var currency by remember(settings) { mutableStateOf(settings.currency) }
    var resinPrice by remember(settings) { mutableStateOf(settings.defaultResinPricePerLitre.trimNumber()) }
    var printer by remember(settings) { mutableStateOf(settings.defaultPrinter) }
    var resinType by remember(settings) { mutableStateOf(settings.defaultResinType) }
    var watts by remember(settings) { mutableStateOf(settings.printerWatts.trimNumber()) }
    var kwhPrice by remember(settings) { mutableStateOf(settings.electricityPricePerKwh.toString()) }
    var includeEnergy by remember(settings) { mutableStateOf(settings.includeEnergyInCost) }
    var machineRate by remember(settings) { mutableStateOf(settings.hourlyMachineRate.trimNumber()) }
    var consumables by remember(settings) { mutableStateOf(settings.consumablesPerPrint.trimNumber()) }

    fun persist() {
        onSave(
            Settings(
                currency = currency.trim().ifBlank { "€" },
                defaultResinPricePerLitre = resinPrice.toDoubleOrZero(),
                defaultPrinter = printer.trim(),
                defaultResinType = resinType.trim().ifBlank { "Résine standard" },
                printerWatts = watts.toDoubleOrZero(),
                electricityPricePerKwh = kwhPrice.toDoubleOrZero(),
                includeEnergyInCost = includeEnergy,
                hourlyMachineRate = machineRate.toDoubleOrZero(),
                consumablesPerPrint = consumables.toDoubleOrZero()
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
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
                SectionHeader("Valeurs par défaut", subtitle = "Pré-remplissent chaque nouveau plateau")
                Spacer(Modifier.height(10.dp))
                TwoColumns(
                    left = { FormTextField("Imprimante", printer, { printer = it }) },
                    right = { FormTextField("Résine", resinType, { resinType = it }) }
                )
                Spacer(Modifier.height(10.dp))
                TwoColumns(
                    left = {
                        FormTextField(
                            "Prix résine", resinPrice, { resinPrice = it.filterDecimal() },
                            keyboardType = KeyboardType.Decimal, suffix = "/L"
                        )
                    },
                    right = { FormTextField("Devise", currency, { currency = it.take(3) }) }
                )
            }

            AppCard {
                SectionHeader("Électricité et consommables", subtitle = "Intégrés au coût de chaque plateau")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Compter l'électricité", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Puissance × durée × prix du kWh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = includeEnergy, onCheckedChange = { includeEnergy = it })
                }
                if (includeEnergy) {
                    Spacer(Modifier.height(12.dp))
                    TwoColumns(
                        left = {
                            FormTextField(
                                "Puissance", watts, { watts = it.filterDecimal() },
                                keyboardType = KeyboardType.Decimal, suffix = "W"
                            )
                        },
                        right = {
                            FormTextField(
                                "Prix kWh", kwhPrice, { kwhPrice = it.filterDecimal() },
                                keyboardType = KeyboardType.Decimal, suffix = currency
                            )
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                FormTextField(
                    "Coût machine horaire", machineRate, { machineRate = it.filterDecimal() },
                    placeholder = "Amortissement, écran LCD… (0 pour ignorer)",
                    keyboardType = KeyboardType.Decimal, suffix = "$currency/h"
                )
                Spacer(Modifier.height(10.dp))
                FormTextField(
                    "Lavage et durcissement", consumables, { consumables = it.filterDecimal() },
                    placeholder = "Alcool, gants, papier… par plateau (0 pour ignorer)",
                    keyboardType = KeyboardType.Decimal, suffix = "$currency"
                )
            }

            AppCard {
                SectionHeader("Notifications", subtitle = "Pour les rappels de plateau")
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (notificationsAllowed) "Notifications autorisées."
                    else "Les notifications sont bloquées : les rappels ne s'afficheront pas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notificationsAllowed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
                if (!notificationsAllowed) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRequestNotifications) { Text("Autoriser") }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (exactAlarmsAllowed)
                        "Les rappels se déclenchent à l'heure exacte."
                    else "Sans l'autorisation « alarmes exactes », les rappels peuvent arriver avec du retard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!exactAlarmsAllowed) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenExactAlarmSettings) { Text("Régler l'exactitude") }
                }
            }

            Button(
                onClick = { persist(); onBack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Enregistrer") }

            Text(
                text = "Organisator · version $appVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
