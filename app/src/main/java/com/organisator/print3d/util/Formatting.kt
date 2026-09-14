package com.organisator.print3d.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val FR = Locale.FRANCE
private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", FR)
private val dateTimeFmt = DateTimeFormatter.ofPattern("d MMM · HH:mm", FR)
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", FR)
private val dayShortFmt = DateTimeFormatter.ofPattern("d MMM", FR)

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatDate(millis: Long): String = millis.toLocalDateTime().format(dateFmt)
fun formatDateTime(millis: Long): String = millis.toLocalDateTime().format(dateTimeFmt)
fun formatTime(millis: Long): String = millis.toLocalDateTime().format(timeFmt)
fun formatDayShort(date: LocalDate): String = date.format(dayShortFmt)

/** « 14 h 05 », « 45 min », « — » : format court et lisible d'une durée en minutes. */
fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "—"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h ${m.toString().padStart(2, '0')}"
    }
}

/** Durée exprimée en heures décimales, pour les graphiques. */
fun minutesToHours(minutes: Int): Double = minutes / 60.0

fun formatMoney(amount: Double, currency: String): String {
    val rounded = (amount * 100).roundToInt() / 100.0
    return String.format(FR, "%,.2f %s", rounded, currency)
}

fun formatMillilitres(ml: Double): String = when {
    ml <= 0 -> "—"
    ml >= 1000 -> String.format(FR, "%.2f L", ml / 1000.0)
    else -> String.format(FR, "%.0f mL", ml)
}

/** « dans 3 h », « il y a 2 j », pour les rappels et les échéances. */
fun formatRelative(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diff = targetMillis - nowMillis
    val past = diff < 0
    val minutes = abs(diff) / 60_000
    val text = when {
        minutes < 1 -> "à l'instant"
        minutes < 60 -> "$minutes min"
        minutes < 60 * 24 -> "${minutes / 60} h"
        minutes < 60 * 24 * 30 -> "${minutes / (60 * 24)} j"
        else -> "${minutes / (60 * 24 * 30)} mois"
    }
    return when {
        minutes < 1 -> text
        past -> "il y a $text"
        else -> "dans $text"
    }
}

/** Convertit « #RRGGBB » en couleur Compose, avec repli si la chaîne est invalide. */
fun parseColor(hex: String, fallback: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color =
    runCatching {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
