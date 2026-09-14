package com.organisator.print3d.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organisator.print3d.ui.theme.AppIcons
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.Settings
import com.organisator.print3d.data.effectiveMinutes
import com.organisator.print3d.data.elapsedMinutes
import com.organisator.print3d.data.progress
import com.organisator.print3d.data.remainingMinutes
import com.organisator.print3d.data.totalCost
import com.organisator.print3d.ui.theme.statusColor
import com.organisator.print3d.util.formatDateTime
import com.organisator.print3d.util.formatMinutes
import com.organisator.print3d.util.formatMoney
import com.organisator.print3d.util.formatRelative

/** Ligne de liste d'un plateau : statut, temps, coût, et avancement si l'impression tourne. */
@Composable
fun JobRow(
    job: PrintJob,
    projectName: String?,
    settings: Settings,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    partsToRedo: Int = 0
) {
    val accent = statusColor(job.status)

    AppCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!projectName.isNullOrBlank() || job.fileName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = listOfNotNull(
                            projectName?.takeIf { it.isNotBlank() },
                            job.fileName.takeIf { it.isNotBlank() }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            StatusChip(job.status, compact = true)
        }

        val progress = job.progress(now)
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            ProgressBar(progress = progress, color = accent)
            Spacer(Modifier.height(6.dp))
            val remaining = job.remainingMinutes(now)
            Text(
                text = buildString {
                    append(formatMinutes(job.elapsedMinutes(now)))
                    append(" / ")
                    append(formatMinutes(job.estimatedMinutes))
                    if (remaining != null) {
                        append(" · ")
                        append(if (remaining == 0) "fin imminente" else "reste ${formatMinutes(remaining)}")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = accent
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetaText(formatMinutes(job.effectiveMinutes(now)))
            MetaText(formatMoney(job.totalCost(settings, now), settings.currency))
            if (job.resinType.isNotBlank()) MetaText(job.resinType)
            if (job.scalePercent != 100) MetaText("${job.scalePercent} %")
        }

        val scheduled = job.scheduledAt
        val reminder = job.reminderAt.takeIf { job.reminderEnabled }
        if (scheduled != null || reminder != null || partsToRedo > 0) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (scheduled != null && job.status != JobStatus.TERMINE) {
                    IconLine(
                        icon = { Icon(AppIcons.Horloge, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = "Prévu ${formatDateTime(scheduled)} · ${formatRelative(scheduled, now)}"
                    )
                }
                if (reminder != null && reminder > now) {
                    IconLine(
                        icon = { Icon(Icons.Outlined.Notifications, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary) },
                        text = "Rappel ${formatRelative(reminder, now)}",
                        highlight = true
                    )
                }
                if (partsToRedo > 0) {
                    Text(
                        text = "$partsToRedo pièce${if (partsToRedo > 1) "s" else ""} à refaire",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor(JobStatus.A_REFAIRE)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

@Composable
private fun IconLine(
    icon: @Composable () -> Unit,
    text: String,
    highlight: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
