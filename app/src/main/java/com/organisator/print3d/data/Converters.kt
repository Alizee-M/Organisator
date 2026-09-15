package com.organisator.print3d.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun jobStatusToString(v: JobStatus): String = v.name
    @TypeConverter fun stringToJobStatus(v: String): JobStatus =
        runCatching { JobStatus.valueOf(v) }.getOrDefault(JobStatus.A_FAIRE)

    @TypeConverter fun projectStatusToString(v: ProjectStatus): String = v.name
    @TypeConverter fun stringToProjectStatus(v: String): ProjectStatus =
        runCatching { ProjectStatus.valueOf(v) }.getOrDefault(ProjectStatus.EN_COURS)

    @TypeConverter fun maintenanceKindToString(v: MaintenanceKind): String = v.name
    @TypeConverter fun stringToMaintenanceKind(v: String): MaintenanceKind =
        runCatching { MaintenanceKind.valueOf(v) }.getOrDefault(MaintenanceKind.AUTRE)

    @TypeConverter fun partStatusToString(v: PartStatus): String = v.name
    @TypeConverter fun stringToPartStatus(v: String): PartStatus =
        runCatching { PartStatus.valueOf(v) }.getOrDefault(PartStatus.A_FAIRE)
}
