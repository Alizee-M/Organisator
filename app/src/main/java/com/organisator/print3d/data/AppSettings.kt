package com.organisator.print3d.data

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Réglages par défaut réutilisés pour chiffrer chaque plateau. */
data class Settings(
    val currency: String = "€",
    val defaultFilamentPricePerKg: Double = 22.0,
    val defaultPrinter: String = "",
    val defaultMaterial: String = "PLA",
    val printerWatts: Double = 120.0,
    val electricityPricePerKwh: Double = 0.2516,
    val includeEnergyInCost: Boolean = true,
    val hourlyMachineRate: Double = 0.0
)

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("organisator_settings", Context.MODE_PRIVATE)

    fun read(): Settings = Settings(
        currency = prefs.getString(KEY_CURRENCY, "€") ?: "€",
        defaultFilamentPricePerKg = prefs.getFloat(KEY_FILAMENT_PRICE, 22.0f).toDouble(),
        defaultPrinter = prefs.getString(KEY_PRINTER, "") ?: "",
        defaultMaterial = prefs.getString(KEY_MATERIAL, "PLA") ?: "PLA",
        printerWatts = prefs.getFloat(KEY_WATTS, 120.0f).toDouble(),
        electricityPricePerKwh = prefs.getFloat(KEY_KWH, 0.2516f).toDouble(),
        includeEnergyInCost = prefs.getBoolean(KEY_ENERGY, true),
        hourlyMachineRate = prefs.getFloat(KEY_MACHINE_RATE, 0.0f).toDouble()
    )

    fun write(settings: Settings) {
        prefs.edit()
            .putString(KEY_CURRENCY, settings.currency)
            .putFloat(KEY_FILAMENT_PRICE, settings.defaultFilamentPricePerKg.toFloat())
            .putString(KEY_PRINTER, settings.defaultPrinter)
            .putString(KEY_MATERIAL, settings.defaultMaterial)
            .putFloat(KEY_WATTS, settings.printerWatts.toFloat())
            .putFloat(KEY_KWH, settings.electricityPricePerKwh.toFloat())
            .putBoolean(KEY_ENERGY, settings.includeEnergyInCost)
            .putFloat(KEY_MACHINE_RATE, settings.hourlyMachineRate.toFloat())
            .apply()
    }

    fun observe(): Flow<Settings> = callbackFlow {
        trySend(read())
        val listener = android.content.SharedPreferences
            .OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val KEY_CURRENCY = "currency"
        const val KEY_FILAMENT_PRICE = "filament_price"
        const val KEY_PRINTER = "printer"
        const val KEY_MATERIAL = "material"
        const val KEY_WATTS = "watts"
        const val KEY_KWH = "kwh"
        const val KEY_ENERGY = "energy"
        const val KEY_MACHINE_RATE = "machine_rate"
    }
}
