package com.organisator.print3d.data

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Réglages par défaut réutilisés pour chiffrer chaque plateau. */
data class Settings(
    val currency: String = "€",
    /** Les bouteilles de résine se vendent au litre ou au demi-litre. */
    val defaultResinPricePerLitre: Double = 45.0,
    val defaultPrinter: String = "",
    val defaultResinType: String = "Résine standard",
    /** Une imprimante résine consomme nettement moins qu'une machine à filament. */
    val printerWatts: Double = 50.0,
    val electricityPricePerKwh: Double = 0.2516,
    val includeEnergyInCost: Boolean = true,
    val hourlyMachineRate: Double = 0.0,
    /** Alcool de lavage, gants, papier absorbant : un coût fixe à chaque plateau. */
    val consumablesPerPrint: Double = 0.0
)

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("organisator_settings", Context.MODE_PRIVATE)

    fun read(): Settings = Settings(
        currency = prefs.getString(KEY_CURRENCY, "€") ?: "€",
        defaultResinPricePerLitre = prefs.getFloat(KEY_RESIN_PRICE, 45.0f).toDouble(),
        defaultPrinter = prefs.getString(KEY_PRINTER, "") ?: "",
        defaultResinType = prefs.getString(KEY_RESIN_TYPE, "Résine standard") ?: "Résine standard",
        printerWatts = prefs.getFloat(KEY_WATTS, 50.0f).toDouble(),
        electricityPricePerKwh = prefs.getFloat(KEY_KWH, 0.2516f).toDouble(),
        includeEnergyInCost = prefs.getBoolean(KEY_ENERGY, true),
        hourlyMachineRate = prefs.getFloat(KEY_MACHINE_RATE, 0.0f).toDouble(),
        consumablesPerPrint = prefs.getFloat(KEY_CONSUMABLES, 0.0f).toDouble()
    )

    fun write(settings: Settings) {
        prefs.edit()
            .putString(KEY_CURRENCY, settings.currency)
            .putFloat(KEY_RESIN_PRICE, settings.defaultResinPricePerLitre.toFloat())
            .putString(KEY_PRINTER, settings.defaultPrinter)
            .putString(KEY_RESIN_TYPE, settings.defaultResinType)
            .putFloat(KEY_WATTS, settings.printerWatts.toFloat())
            .putFloat(KEY_KWH, settings.electricityPricePerKwh.toFloat())
            .putBoolean(KEY_ENERGY, settings.includeEnergyInCost)
            .putFloat(KEY_MACHINE_RATE, settings.hourlyMachineRate.toFloat())
            .putFloat(KEY_CONSUMABLES, settings.consumablesPerPrint.toFloat())
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
        const val KEY_RESIN_PRICE = "resin_price_per_litre"
        const val KEY_PRINTER = "printer"
        const val KEY_RESIN_TYPE = "resin_type"
        const val KEY_WATTS = "watts"
        const val KEY_KWH = "kwh"
        const val KEY_ENERGY = "energy"
        const val KEY_MACHINE_RATE = "machine_rate"
        const val KEY_CONSUMABLES = "consumables"
    }
}
