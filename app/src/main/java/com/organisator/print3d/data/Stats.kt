package com.organisator.print3d.data

import com.organisator.print3d.util.startOfDayMillis
import com.organisator.print3d.util.toLocalDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/** Échelle de temps appliquée aux statistiques. */
enum class StatsRange(val label: String, val days: Int?) {
    WEEK("7 j", 7),
    MONTH("30 j", 30),
    QUARTER("90 j", 90),
    YEAR("1 an", 365),
    ALL("Tout", null)
}

/** Granularité des barres du graphique, déduite de l'échelle choisie. */
enum class Bucket { DAY, WEEK, MONTH }

data class TimePoint(
    val label: String,
    val minutes: Int,
    val cost: Double,
    val jobs: Int
)

data class ResinStat(val resinType: String, val ml: Double, val jobs: Int)

data class ProjectStat(
    val project: Project,
    val total: Int,
    val done: Int,
    val minutes: Int,
    val cost: Double
) {
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
}

data class StatsSummary(
    val range: StatsRange,
    val bucket: Bucket,
    val jobs: Int,
    val finished: Int,
    val redo: Int,
    val running: Int,
    val queued: Int,
    val totalMinutes: Int,
    val totalCost: Double,
    val totalMl: Double,
    val successRate: Float,
    val avgMinutes: Int,
    val avgCost: Double,
    val byStatus: List<Pair<JobStatus, Int>>,
    val timeline: List<TimePoint>,
    val resins: List<ResinStat>,
    val projects: List<ProjectStat>
)

object StatsEngine {

    /** Date de référence d'un plateau : la fin réelle, sinon le lancement, sinon la planification. */
    fun referenceDate(job: PrintJob): Long =
        job.finishedAt ?: job.startedAt ?: job.scheduledAt ?: job.createdAt

    fun bucketFor(range: StatsRange): Bucket = when (range) {
        StatsRange.WEEK, StatsRange.MONTH -> Bucket.DAY
        StatsRange.QUARTER -> Bucket.WEEK
        StatsRange.YEAR, StatsRange.ALL -> Bucket.MONTH
    }

    fun compute(
        allJobs: List<PrintJob>,
        projects: List<Project>,
        settings: Settings,
        range: StatsRange,
        now: Long = System.currentTimeMillis()
    ): StatsSummary {
        val today = now.toLocalDate()
        val from = range.days?.let { today.minusDays((it - 1).toLong()).startOfDayMillis() }
        val jobs = allJobs.filter { job ->
            job.status != JobStatus.ANNULE && (from == null || referenceDate(job) >= from)
        }

        val bucket = bucketFor(range)
        val finished = jobs.count { it.status == JobStatus.TERMINE }
        val redo = jobs.count { it.status == JobStatus.A_REFAIRE }
        val running = jobs.count { it.status == JobStatus.EN_COURS }
        val queued = jobs.count { it.status == JobStatus.A_IMPRIMER || it.status == JobStatus.A_FAIRE }

        val totalMinutes = jobs.sumOf { it.effectiveMinutes(now) }
        val totalCost = jobs.sumOf { it.totalCost(settings, now) }
        val totalMl = jobs.sumOf { it.resinMl }
        val evaluated = finished + redo
        val successRate = if (evaluated == 0) 0f else finished.toFloat() / evaluated

        val byStatus = JobStatus.entries
            .filter { it != JobStatus.ANNULE }
            .map { status -> status to jobs.count { it.status == status } }

        val timeline = buildTimeline(jobs, settings, range, bucket, today, now)

        val resins = jobs
            .groupBy { it.resinType.trim().ifBlank { "Non précisée" } }
            .map { (name, list) ->
                ResinStat(name, list.sumOf { it.resinMl }, list.size)
            }
            .sortedByDescending { it.ml }

        val jobsByProject = jobs.groupBy { it.projectId }
        val projectStats = projects.mapNotNull { project ->
            val list = jobsByProject[project.id].orEmpty()
            if (list.isEmpty()) null else ProjectStat(
                project = project,
                total = list.size,
                done = list.count { it.status == JobStatus.TERMINE },
                minutes = list.sumOf { it.effectiveMinutes(now) },
                cost = list.sumOf { it.totalCost(settings, now) }
            )
        }.sortedByDescending { it.minutes }

        return StatsSummary(
            range = range,
            bucket = bucket,
            jobs = jobs.size,
            finished = finished,
            redo = redo,
            running = running,
            queued = queued,
            totalMinutes = totalMinutes,
            totalCost = totalCost,
            totalMl = totalMl,
            successRate = successRate,
            avgMinutes = if (jobs.isEmpty()) 0 else totalMinutes / jobs.size,
            avgCost = if (jobs.isEmpty()) 0.0 else totalCost / jobs.size,
            byStatus = byStatus,
            timeline = timeline,
            resins = resins,
            projects = projectStats
        )
    }

    private fun buildTimeline(
        jobs: List<PrintJob>,
        settings: Settings,
        range: StatsRange,
        bucket: Bucket,
        today: LocalDate,
        now: Long
    ): List<TimePoint> {
        if (jobs.isEmpty() && range == StatsRange.ALL) return emptyList()

        val earliest = jobs.minOfOrNull { referenceDate(it).toLocalDate() } ?: today
        val start = when (range.days) {
            null -> earliest
            else -> today.minusDays((range.days - 1).toLong())
        }

        val keys = mutableListOf<LocalDate>()
        when (bucket) {
            Bucket.DAY -> {
                var d = start
                while (!d.isAfter(today)) { keys += d; d = d.plusDays(1) }
            }
            Bucket.WEEK -> {
                val weekFields = WeekFields.of(Locale.FRANCE)
                var d = start.with(weekFields.dayOfWeek(), 1)
                while (!d.isAfter(today)) { keys += d; d = d.plusWeeks(1) }
            }
            Bucket.MONTH -> {
                var d = start.withDayOfMonth(1)
                while (!d.isAfter(today)) { keys += d; d = d.plusMonths(1) }
            }
        }
        // Une échelle « Tout » sur plusieurs années resterait illisible : on borne.
        val bounded = if (keys.size > MAX_POINTS) keys.takeLast(MAX_POINTS) else keys

        val grouped = jobs.groupBy { job ->
            val date = referenceDate(job).toLocalDate()
            when (bucket) {
                Bucket.DAY -> date
                Bucket.WEEK -> date.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 1)
                Bucket.MONTH -> date.withDayOfMonth(1)
            }
        }

        return bounded.map { key ->
            val list = grouped[key].orEmpty()
            TimePoint(
                label = labelFor(key, bucket),
                minutes = list.sumOf { it.effectiveMinutes(now) },
                cost = list.sumOf { it.totalCost(settings, now) },
                jobs = list.size
            )
        }
    }

    private fun labelFor(date: LocalDate, bucket: Bucket): String = when (bucket) {
        Bucket.DAY -> date.dayOfMonth.toString()
        Bucket.WEEK -> "S${date.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear())}"
        Bucket.MONTH -> MONTHS[date.monthValue - 1]
    }

    /** Nombre de jours couverts par l'échelle, pour l'afficher sous le graphique. */
    fun spanDays(range: StatsRange, jobs: List<PrintJob>): Long {
        range.days?.let { return it.toLong() }
        val earliest = jobs.minOfOrNull { referenceDate(it) } ?: return 0
        return ChronoUnit.DAYS.between(earliest.toLocalDate(), LocalDate.now()) + 1
    }

    private const val MAX_POINTS = 31
    private val MONTHS = listOf(
        "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
        "Juil", "Août", "Sep", "Oct", "Nov", "Déc"
    )
}
