package com.organisator.print3d.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Où en est un plateau dans le cycle de vie d'une impression. */
enum class JobStatus(val label: String) {
    A_FAIRE("À faire"),
    A_IMPRIMER("À imprimer"),
    EN_COURS("En cours"),
    TERMINE("Terminé"),
    A_REFAIRE("À refaire"),
    ANNULE("Annulé");

    val isOpen: Boolean get() = this == A_FAIRE || this == A_IMPRIMER || this == EN_COURS || this == A_REFAIRE
}

enum class ProjectStatus(val label: String) {
    EN_COURS("En cours"),
    EN_PAUSE("En pause"),
    TERMINE("Terminé"),
    ARCHIVE("Archivé")
}

enum class PartStatus(val label: String) {
    A_FAIRE("À faire"),
    OK("Imprimée"),
    A_REFAIRE("À refaire")
}

/** Le parc de l'atelier : les plateaux se rattachent à l'une de ces machines. */
val PRINTERS = listOf(
    "M7 Max",
    "M7 Pro Gris Gauche",
    "M7 Pro Gris Droite",
    "M7 Pro Clear"
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#6C7BFF",
    val status: ProjectStatus = ProjectStatus.EN_COURS,
    val deadline: Long? = null,
    /**
     * Chemin absolu de la photo du projet, recopiée dans le stockage privé de
     * l'application : les URI rendus par le sélecteur système ne restent pas
     * lisibles après le redémarrage du téléphone.
     */
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Un plateau d'impression : l'unité de travail suivie par l'application.
 * Un plateau peut contenir plusieurs pièces (voir [PrintPart]).
 */
@Entity(
    tableName = "print_jobs",
    indices = [Index("projectId"), Index("status"), Index("scheduledAt")],
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.SET_NULL
    )]
)
data class PrintJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val projectId: Long? = null,
    val fileName: String = "",
    val printer: String = "",
    /** Type de résine employée (standard, ABS-like, water-washable…). */
    val resinType: String = "Résine standard",
    val resinColor: String = "",
    val status: JobStatus = JobStatus.A_FAIRE,
    /** Échelle d'impression appliquée au modèle, en pourcentage. */
    val scalePercent: Int = 100,
    /** Nombre d'exemplaires posés sur le plateau. */
    val quantity: Int = 1,
    /** Hauteur de couche, en microns : la granularité usuelle en résine. */
    val layerHeightMicrons: Int = 50,
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val scheduledAt: Long? = null,
    val reminderAt: Long? = null,
    val reminderEnabled: Boolean = false,
    /** Volume de résine consommé, en millilitres. */
    val resinMl: Double = 0.0,
    /** Prix de la résine au litre, conditionnement habituel des bouteilles. */
    val resinPricePerLitre: Double = 0.0,
    /** Coûts annexes : supports, ponçage, apprêt, peinture… */
    val extraCost: Double = 0.0,
    /** null tant que le plateau n'a pas été évalué. */
    val plateFullyOk: Boolean? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** Nature d'une intervention d'entretien sur une machine. */
enum class MaintenanceKind(val label: String) {
    FILM_FEP("Film FEP"),
    ECRAN_LCD("Écran LCD"),
    NETTOYAGE("Nettoyage"),
    CALIBRATION("Calibration"),
    PLATEAU("Plateau"),
    RESINE("Bidon de résine"),
    AUTRE("Autre")
}

/**
 * Une intervention sur une imprimante. Le compteur de couches relevé ce jour-là
 * sert de repère d'usure : c'est lui qui dit quand le prochain changement
 * approche, plus sûrement que la date seule.
 */
@Entity(
    tableName = "maintenance",
    indices = [Index("printer"), Index("date")]
)
data class MaintenanceEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val printer: String,
    val kind: MaintenanceKind = MaintenanceKind.FILM_FEP,
    val date: Long = System.currentTimeMillis(),
    /** Compteur de couches de la machine au moment de l'intervention. */
    val layerCount: Int = 0,
    val notes: String = ""
)

@Entity(
    tableName = "print_parts",
    indices = [Index("jobId")],
    foreignKeys = [ForeignKey(
        entity = PrintJob::class,
        parentColumns = ["id"],
        childColumns = ["jobId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PrintPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val name: String,
    val quantity: Int = 1,
    val status: PartStatus = PartStatus.A_FAIRE,
    val notes: String = ""
)
