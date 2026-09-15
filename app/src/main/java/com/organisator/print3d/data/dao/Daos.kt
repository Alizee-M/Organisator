package com.organisator.print3d.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.organisator.print3d.data.MaintenanceEntry
import com.organisator.print3d.data.PrintJob
import com.organisator.print3d.data.PrintPart
import com.organisator.print3d.data.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: Long): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)
}

@Dao
interface PrintJobDao {
    @Query("SELECT * FROM print_jobs ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PrintJob>>

    @Query("SELECT * FROM print_jobs WHERE id = :id")
    fun observeById(id: Long): Flow<PrintJob?>

    @Query("SELECT * FROM print_jobs WHERE id = :id")
    suspend fun getById(id: Long): PrintJob?

    @Query("SELECT * FROM print_jobs WHERE reminderEnabled = 1 AND reminderAt IS NOT NULL AND reminderAt > :after")
    suspend fun getPendingReminders(after: Long): List<PrintJob>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: PrintJob): Long

    @Update
    suspend fun update(job: PrintJob)

    @Delete
    suspend fun delete(job: PrintJob)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance ORDER BY date DESC")
    fun observeAll(): Flow<List<MaintenanceEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MaintenanceEntry): Long

    @Update
    suspend fun update(entry: MaintenanceEntry)

    @Delete
    suspend fun delete(entry: MaintenanceEntry)
}

@Dao
interface PrintPartDao {
    @Query("SELECT * FROM print_parts ORDER BY id ASC")
    fun observeAll(): Flow<List<PrintPart>>

    @Query("SELECT * FROM print_parts WHERE jobId = :jobId ORDER BY id ASC")
    fun observeForJob(jobId: Long): Flow<List<PrintPart>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(part: PrintPart): Long

    @Update
    suspend fun update(part: PrintPart)

    @Delete
    suspend fun delete(part: PrintPart)

    @Query("DELETE FROM print_parts WHERE jobId = :jobId")
    suspend fun deleteForJob(jobId: Long)
}
