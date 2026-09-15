package com.organisator.print3d.data

import android.content.Context
import android.net.Uri
import com.organisator.print3d.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Point d'entrée unique pour lire et modifier les données de l'application. */
class PrintRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val jobDao = db.printJobDao()
    private val projectDao = db.projectDao()
    private val partDao = db.printPartDao()
    private val maintenanceDao = db.maintenanceDao()
    private val scheduler = ReminderScheduler(context)

    val settingsStore = SettingsStore(context)
    private val photoStore = PhotoStore(context)

    fun observeJobs(): Flow<List<PrintJob>> = jobDao.observeAll()
    fun observeProjects(): Flow<List<Project>> = projectDao.observeAll()
    fun observeParts(): Flow<List<PrintPart>> = partDao.observeAll()
    fun observeMaintenance(): Flow<List<MaintenanceEntry>> = maintenanceDao.observeAll()
    fun observePartsForJob(jobId: Long): Flow<List<PrintPart>> = partDao.observeForJob(jobId)
    fun observeJob(id: Long): Flow<PrintJob?> = jobDao.observeById(id)

    suspend fun upsertJob(job: PrintJob): Long {
        val stamped = job.copy(updatedAt = System.currentTimeMillis())
        val id = if (stamped.id == 0L) {
            jobDao.insert(stamped)
        } else {
            jobDao.update(stamped)
            stamped.id
        }
        syncReminder(stamped.copy(id = id))
        return id
    }

    suspend fun deleteJob(job: PrintJob) {
        scheduler.cancel(job.id)
        jobDao.delete(job)
    }

    suspend fun updateStatus(job: PrintJob, status: JobStatus) {
        val now = System.currentTimeMillis()
        var updated = job.copy(status = status, updatedAt = now)
        when (status) {
            JobStatus.EN_COURS -> updated = updated.copy(
                startedAt = job.startedAt ?: now,
                finishedAt = null
            )
            JobStatus.TERMINE, JobStatus.A_REFAIRE -> {
                val finished = now
                val elapsed = job.startedAt?.let { ((finished - it) / 60_000L).toInt() } ?: 0
                updated = updated.copy(
                    finishedAt = finished,
                    actualMinutes = if (job.actualMinutes > 0) job.actualMinutes else elapsed,
                    plateFullyOk = job.plateFullyOk ?: (status == JobStatus.TERMINE)
                )
            }
            else -> Unit
        }
        jobDao.update(updated)
        if (!status.isOpen || status == JobStatus.TERMINE) scheduler.cancel(job.id)
    }

    suspend fun upsertProject(project: Project): Long =
        if (project.id == 0L) projectDao.insert(project) else {
            projectDao.update(project); project.id
        }

    suspend fun deleteProject(project: Project) {
        photoStore.delete(project.photoPath)
        projectDao.delete(project)
    }

    /** Importe une photo et renvoie son chemin local, ou null si la lecture échoue. */
    fun importPhoto(uri: Uri): String? = photoStore.import(uri)

    fun cropPhoto(path: String, crop: PhotoCrop): String? = photoStore.crop(path, crop)

    fun deletePhoto(path: String?) = photoStore.delete(path)

    /** Efface les photos qu'aucun projet ne référence (import abandonné, projet supprimé). */
    suspend fun cleanupOrphanPhotos() {
        val referenced = projectDao.observeAll().first().mapNotNull { it.photoPath }
        photoStore.removeOrphans(referenced)
    }

    suspend fun upsertPart(part: PrintPart): Long =
        if (part.id == 0L) partDao.insert(part) else {
            partDao.update(part); part.id
        }

    suspend fun deletePart(part: PrintPart) = partDao.delete(part)

    /**
     * Découpe une saisie libre en pièces : « tête bras jambes » donne trois
     * entrées. Les doublons déjà présents sur le plateau sont ignorés.
     */
    suspend fun addParts(jobId: Long, raw: String) {
        val existing = partDao.observeForJob(jobId).first().map { it.name.lowercase() }.toHashSet()
        raw.split(SEPARATORS)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { name ->
                if (existing.add(name.lowercase())) {
                    partDao.insert(PrintPart(jobId = jobId, name = name, status = PartStatus.OK))
                }
            }
    }

    suspend fun upsertMaintenance(entry: MaintenanceEntry): Long =
        if (entry.id == 0L) maintenanceDao.insert(entry) else {
            maintenanceDao.update(entry); entry.id
        }

    suspend fun deleteMaintenance(entry: MaintenanceEntry) = maintenanceDao.delete(entry)

    private companion object {
        /** Espaces avant tout, mais virgules et points-virgules dépannent aussi. */
        val SEPARATORS = Regex("[\\s,;]+")
    }

    /** Réarme tous les rappels encore à venir (après un redémarrage de l'appareil). */
    suspend fun rescheduleAllReminders() {
        jobDao.getPendingReminders(System.currentTimeMillis()).forEach { syncReminder(it) }
    }

    private fun syncReminder(job: PrintJob) {
        val at = job.reminderAt
        if (job.reminderEnabled && at != null && at > System.currentTimeMillis() && job.status.isOpen) {
            scheduler.schedule(job.id, job.name, at)
        } else {
            scheduler.cancel(job.id)
        }
    }
}
