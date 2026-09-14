package com.organisator.print3d.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max

/**
 * Histogramme sobre : barres arrondies, deux lignes de repère, libellés espacés
 * automatiquement pour rester lisibles quelle que soit l'échelle choisie.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    chartHeight: Dp = 150.dp,
    topLabel: String? = null
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val emptyBarColor = MaterialTheme.colorScheme.outlineVariant
    val maxValue = max(values.maxOrNull() ?: 0f, 0.0001f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (topLabel != null) {
            Text(
                text = topLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
            Spacer(Modifier.height(6.dp))
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val w = size.width
            val h = size.height
            if (values.isEmpty() || w <= 0f) return@Canvas

            // Repères horizontaux : plafond et moitié.
            listOf(0f, 0.5f).forEach { fraction ->
                val y = h * fraction
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            val slot = w / values.size
            val barWidth = (slot * 0.58f).coerceAtMost(26.dp.toPx())
            val radius = CornerRadius(barWidth / 2.6f, barWidth / 2.6f)

            values.forEachIndexed { index, value ->
                val ratio = (value / maxValue).coerceIn(0f, 1f)
                val barHeight = max(ratio * (h - 2f), if (value > 0f) 3f else 2f)
                val left = index * slot + (slot - barWidth) / 2f
                drawRoundRect(
                    color = if (value > 0f) barColor else emptyBarColor,
                    topLeft = Offset(left, h - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius
                )
            }

            drawLine(
                color = gridColor,
                start = Offset(0f, h),
                end = Offset(w, h),
                strokeWidth = 2f
            )
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            val step = max(1, ceil(labels.size / 8.0).toInt())
            Row(modifier = Modifier.fillMaxWidth()) {
                labels.forEachIndexed { index, label ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (index % step == 0 || index == labels.lastIndex) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DonutSegment(val label: String, val value: Float, val color: Color)

/** Anneau de répartition avec une valeur mise en avant au centre. */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 136.dp
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = size.minDimension * 0.15f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            if (total <= 0f) return@Canvas
            var start = -90f
            segments.forEach { segment ->
                if (segment.value <= 0f) return@forEach
                val sweep = segment.value / total * 360f
                drawArc(
                    color = segment.color,
                    startAngle = start + GAP_DEGREES / 2f,
                    sweepAngle = (sweep - GAP_DEGREES).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LegendRow(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Barre de progression fine, utilisée pour les plateaux et les projets. */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 6.dp
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
    }
}

private const val GAP_DEGREES = 3f
