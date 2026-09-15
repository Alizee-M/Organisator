package com.organisator.print3d.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.PartStatus
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.PrintPart
import com.organisator.print3d.data.PhotoCrop
import com.organisator.print3d.data.PrintRepository
import com.organisator.print3d.data.Project
import com.organisator.print3d.data.Settings
import com.organisator.print3d.data.StatsEngine
import com.organisator.print3d.data.StatsRange
import com.organisator.print3d.data.StatsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppState(
    val jobs: List<PrintJob> = emptyList(),
    val projects: List<Project> = emptyList(),
    val parts: List<PrintPart> = emptyList(),
    val settings: Settings = Settings(),
    val loaded: Boolean = false
)

class AppViewModel(private val repository: PrintRepository) : ViewModel() {

    init {
        // Une seule fois par instance : la rotation de l'écran ne doit pas relancer
        // le ménage, qui effacerait une photo importée mais pas encore enregistrée.
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.cleanupOrphanPhotos() }
        }
    }

    /** Horloge partagée : fait vivre les compteurs des impressions en cours. */
    val now: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    val state: StateFlow<AppState> = combine(
        repository.observeJobs(),
        repository.observeProjects(),
        repository.observeParts(),
        repository.settingsStore.observe()
    ) { jobs, projects, parts, settings ->
        AppState(jobs, projects, parts, settings, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())

    private val _statsRange = MutableStateFlow(StatsRange.MONTH)
    val statsRange: StateFlow<StatsRange> = _statsRange

    val stats: StateFlow<StatsSummary> = combine(state, _statsRange, now) { s, range, clock ->
        StatsEngine.compute(s.jobs, s.projects, s.settings, range, clock)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatsEngine.compute(emptyList(), emptyList(), Settings(), StatsRange.MONTH)
    )

    fun setStatsRange(range: StatsRange) { _statsRange.value = range }

    // --- Plateaux -----------------------------------------------------------

    fun saveJob(job: PrintJob, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        onSaved(repository.upsertJob(job))
    }

    fun deleteJob(job: PrintJob) = viewModelScope.launch { repository.deleteJob(job) }

    fun setStatus(job: PrintJob, status: JobStatus) = viewModelScope.launch {
        repository.updateStatus(job, status)
    }

    fun setPlateOutcome(job: PrintJob, fullyOk: Boolean) = viewModelScope.launch {
        repository.upsertJob(
            job.copy(
                plateFullyOk = fullyOk,
                status = if (fullyOk) JobStatus.TERMINE else JobStatus.A_REFAIRE,
                finishedAt = job.finishedAt ?: System.currentTimeMillis(),
                actualMinutes = if (job.actualMinutes > 0) job.actualMinutes else {
                    job.startedAt?.let { ((System.currentTimeMillis() - it) / 60_000L).toInt() } ?: 0
                }
            )
        )
    }

    /** Reprogramme un plateau raté : on repart d'une copie propre, à réimprimer. */
    fun duplicateForReprint(job: PrintJob, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val copy = job.copy(
            id = 0,
            name = job.name + " (reprise)",
            status = JobStatus.A_IMPRIMER,
            startedAt = null,
            finishedAt = null,
            actualMinutes = 0,
            plateFullyOk = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        onCreated(repository.upsertJob(copy))
    }

    // --- Projets ------------------------------------------------------------

    fun saveProject(project: Project, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        onSaved(repository.upsertProject(project))
    }

    fun deleteProject(project: Project) = viewModelScope.launch { repository.deleteProject(project) }

    /**
     * Recopie la photo choisie hors du fil principal, puis rend son chemin local.
     * [onImported] reçoit null si l'image n'a pas pu être lue.
     */
    fun importProjectPhoto(uri: Uri, onImported: (String?) -> Unit) = viewModelScope.launch {
        onImported(withContext(Dispatchers.IO) { repository.importPhoto(uri) })
    }

    /** Écrit la zone retenue dans un nouveau fichier et rend son chemin. */
    fun cropProjectPhoto(path: String, crop: PhotoCrop, onCropped: (String?) -> Unit) =
        viewModelScope.launch {
            onCropped(withContext(Dispatchers.IO) { repository.cropPhoto(path, crop) })
        }

    fun discardProjectPhoto(path: String?) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repository.deletePhoto(path) }
    }

    // --- Pièces -------------------------------------------------------------

    fun savePart(part: PrintPart) = viewModelScope.launch { repository.upsertPart(part) }

    fun deletePart(part: PrintPart) = viewModelScope.launch { repository.deletePart(part) }

    fun cyclePartStatus(part: PrintPart) = viewModelScope.launch {
        val next = when (part.status) {
            PartStatus.A_FAIRE -> PartStatus.OK
            PartStatus.OK -> PartStatus.A_REFAIRE
            PartStatus.A_REFAIRE -> PartStatus.A_FAIRE
        }
        repository.upsertPart(part.copy(status = next))
    }

    // --- Réglages -----------------------------------------------------------

    fun saveSettings(settings: Settings) {
        repository.settingsStore.write(settings)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(PrintRepository(context.applicationContext)) as T
    }

    private companion object {
        const val TICK_MS = 30_000L
    }
}
