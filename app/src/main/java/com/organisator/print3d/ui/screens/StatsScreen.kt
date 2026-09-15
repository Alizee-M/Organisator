package com.organisator.print3d.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organisator.print3d.ui.theme.AppIcons
import com.organisator.print3d.data.Bucket
import com.organisator.print3d.data.Settings
import com.organisator.print3d.data.StatsRange
import com.organisator.print3d.data.StatsSummary
import com.organisator.print3d.ui.components.AppCard
import com.organisator.print3d.ui.components.BarChart
import com.organisator.print3d.ui.components.ChipSelector
import com.organisator.print3d.ui.components.DonutChart
import com.organisator.print3d.ui.components.DonutSegment
import com.organisator.print3d.ui.components.EmptyState
import com.organisator.print3d.ui.components.LegendRow
import com.organisator.print3d.ui.components.ProgressBar
import com.organisator.print3d.ui.components.SectionHeader
import com.organisator.print3d.ui.components.StatTile
import com.organisator.print3d.ui.theme.StatusPalette
import com.organisator.print3d.ui.theme.isDarkTheme
import com.organisator.print3d.util.formatMillilitres
import com.organisator.print3d.util.formatMinutes
import com.organisator.print3d.util.formatMoney
import com.organisator.print3d.util.parseColor
import kotlin.math.roundToInt

private enum class Metric(val label: String) {
    TEMPS("Temps"), COUT("Coût"), PLATEAUX("Plateaux")
}

@Composable
fun StatsScreen(
    stats: StatsSummary,
    settings: Settings,
    range: StatsRange,
    onRangeChange: (StatsRange) -> Unit,
    hasData: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var metric by rememberSaveable { mutableStateOf(Metric.TEMPS) }
    val dark = isDarkTheme()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Statistiques", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Échelle de temps appliquée à tous les chiffres ci-dessous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ChipSelector(
                options = StatsRange.entries.toList(),
                selected = range,
                onSelect = onRangeChange,
                labelOf = { it.label }
            )
        }

        if (!hasData) {
            item {
                EmptyState(
                    icon = AppIcons.Stats,
                    title = "Pas encore de données",
                    message = "Enregistrez vos plateaux : temps, coûts et taux de réussite apparaîtront ici."
                )
            }
            return@LazyColumn
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Temps d'impression",
                    value = formatMinutes(stats.totalMinutes),
                    caption = "Moy. ${formatMinutes(stats.avgMinutes)} / plateau",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Coût total",
                    value = formatMoney(stats.totalCost, settings.currency),
                    caption = "Moy. ${formatMoney(stats.avgCost, settings.currency)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Plateaux",
                    value = stats.jobs.toString(),
                    caption = "${stats.finished} terminés · ${stats.redo} à refaire",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Résine",
                    value = formatMillilitres(stats.totalMl),
                    caption = "Consommé sur la période",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            AppCard {
                SectionHeader(
                    title = when (metric) {
                        Metric.TEMPS -> "Temps d'impression"
                        Metric.COUT -> "Coût"
                        Metric.PLATEAUX -> "Plateaux lancés"
                    },
                    subtitle = bucketLabel(stats.bucket)
                )
                Spacer(Modifier.height(10.dp))
                ChipSelector(
                    options = Metric.entries.toList(),
                    selected = metric,
                    onSelect = { metric = it },
                    labelOf = { it.label }
                )
                Spacer(Modifier.height(14.dp))
                val values = stats.timeline.map {
                    when (metric) {
                        Metric.TEMPS -> (it.minutes / 60f)
                        Metric.COUT -> it.cost.toFloat()
                        Metric.PLATEAUX -> it.jobs.toFloat()
                    }
                }
                val peak = values.maxOrNull() ?: 0f
                BarChart(
                    values = values,
                    labels = stats.timeline.map { it.label },
                    topLabel = when (metric) {
                        Metric.TEMPS -> "max ${formatMinutes((peak * 60).roundToInt())}"
                        Metric.COUT -> "max ${formatMoney(peak.toDouble(), settings.currency)}"
                        Metric.PLATEAUX -> "max ${peak.roundToInt()}"
                    }
                )
            }
        }

        item {
            AppCard {
                SectionHeader("Répartition", subtitle = "Par statut de plateau")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val segments = stats.byStatus
                        .filter { it.second > 0 }
                        .map { (status, count) ->
                            DonutSegment(status.label, count.toFloat(), StatusPalette.color(status, dark))
                        }
                    DonutChart(
                        segments = segments,
                        centerValue = "${(stats.successRate * 100).roundToInt()} %",
                        centerLabel = "réussite"
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        stats.byStatus.forEach { (status, count) ->
                            LegendRow(
                                label = status.label,
                                value = count.toString(),
                                color = StatusPalette.color(status, dark)
                            )
                        }
                    }
                }
            }
        }

        if (stats.resins.isNotEmpty()) {
            item {
                AppCard {
                    SectionHeader("Résines", subtitle = "Volume consommé")
                    Spacer(Modifier.height(10.dp))
                    val maxMl = stats.resins.maxOf { it.ml }.coerceAtLeast(0.001)
                    val palette = StatusPalette.chartSeries(dark)
                    stats.resins.forEachIndexed { index, resin ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    resin.resinType,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${formatMillilitres(resin.ml)} · ${resin.jobs}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            ProgressBar(
                                progress = (resin.ml / maxMl).toFloat(),
                                color = palette[index % palette.size],
                                height = 5.dp
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        if (stats.projects.isNotEmpty()) {
            item {
                AppCard {
                    SectionHeader("Projets", subtitle = "Avancement et temps cumulé")
                    Spacer(Modifier.height(10.dp))
                    stats.projects.take(6).forEach { stat ->
                        val color = parseColor(stat.project.colorHex, MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    stat.project.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    "${stat.done}/${stat.total} · ${formatMinutes(stat.minutes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            ProgressBar(progress = stat.progress, color = color, height = 5.dp)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        item {
            AppCard {
                SectionHeader("Synthèse")
                Spacer(Modifier.height(8.dp))
                com.organisator.print3d.ui.components.DetailRow(
                    "Taux de réussite",
                    "${(stats.successRate * 100).roundToInt()} %"
                )
                com.organisator.print3d.ui.components.DetailRow("En cours", stats.running.toString())
                com.organisator.print3d.ui.components.DetailRow("En attente", stats.queued.toString())
                com.organisator.print3d.ui.components.DetailRow("À refaire", stats.redo.toString())
                com.organisator.print3d.ui.components.DetailRow(
                    "Coût horaire moyen",
                    if (stats.totalMinutes > 0)
                        formatMoney(stats.totalCost / (stats.totalMinutes / 60.0), settings.currency) + " / h"
                    else "—"
                )
            }
        }
    }
}

private fun bucketLabel(bucket: Bucket): String = when (bucket) {
    Bucket.DAY -> "Par jour"
    Bucket.WEEK -> "Par semaine"
    Bucket.MONTH -> "Par mois"
}
