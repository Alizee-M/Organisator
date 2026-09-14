package com.organisator.print3d.data

import kotlin.math.max
import kotlin.math.min

/** Décomposition du coût d'un plateau, pour l'afficher poste par poste. */
data class CostBreakdown(
    val resin: Double,
    val energy: Double,
    val machine: Double,
    val consumables: Double,
    val extra: Double
) {
    val total: Double get() = resin + energy + machine + consumables + extra
}

/**
 * Durée retenue pour les statistiques : le réel s'il est connu, sinon le temps
 * déjà écoulé pour une impression en cours, sinon l'estimation.
 */
fun PrintJob.effectiveMinutes(now: Long = System.currentTimeMillis()): Int = when {
    actualMinutes > 0 -> actualMinutes
    status == JobStatus.EN_COURS && startedAt != null ->
        max(0, ((now - startedAt) / 60_000L).toInt())
    else -> estimatedMinutes
}

/** Minutes réellement écoulées depuis le lancement, ou 0 si le plateau n'a pas démarré. */
fun PrintJob.elapsedMinutes(now: Long = System.currentTimeMillis()): Int {
    val start = startedAt ?: return 0
    val end = finishedAt ?: now
    return max(0, ((end - start) / 60_000L).toInt())
}

/** Avancement d'une impression en cours, borné à 1, ou null si non calculable. */
fun PrintJob.progress(now: Long = System.currentTimeMillis()): Float? {
    if (status != JobStatus.EN_COURS || startedAt == null || estimatedMinutes <= 0) return null
    val elapsed = elapsedMinutes(now).toFloat()
    return min(1f, max(0f, elapsed / estimatedMinutes.toFloat()))
}

/** Minutes restantes estimées pour une impression en cours (0 si dépassée). */
fun PrintJob.remainingMinutes(now: Long = System.currentTimeMillis()): Int? {
    if (status != JobStatus.EN_COURS || startedAt == null || estimatedMinutes <= 0) return null
    return max(0, estimatedMinutes - elapsedMinutes(now))
}

fun PrintJob.costBreakdown(settings: Settings, now: Long = System.currentTimeMillis()): CostBreakdown {
    val pricePerLitre = if (resinPricePerLitre > 0) resinPricePerLitre else settings.defaultResinPricePerLitre
    val resin = resinMl / 1000.0 * pricePerLitre
    val hours = effectiveMinutes(now) / 60.0
    val energy = if (settings.includeEnergyInCost) {
        settings.printerWatts / 1000.0 * hours * settings.electricityPricePerKwh
    } else 0.0
    val machine = settings.hourlyMachineRate * hours
    return CostBreakdown(
        resin = resin,
        energy = energy,
        machine = machine,
        consumables = settings.consumablesPerPrint,
        extra = extraCost
    )
}

fun PrintJob.totalCost(settings: Settings, now: Long = System.currentTimeMillis()): Double =
    costBreakdown(settings, now).total
