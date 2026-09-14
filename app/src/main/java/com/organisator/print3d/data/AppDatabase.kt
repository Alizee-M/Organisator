package com.organisator.print3d.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.organisator.print3d.data.dao.PrintJobDao
import com.organisator.print3d.data.dao.PrintPartDao
import com.organisator.print3d.data.dao.ProjectDao

@Database(
    entities = [Project::class, PrintJob::class, PrintPart::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun printJobDao(): PrintJobDao
    abstract fun printPartDao(): PrintPartDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "organisator.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        /** Ajout de la photo de projet. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `photoPath` TEXT")
            }
        }

        /**
         * Passage du filament à la résine. SQLite ne sait pas renommer des colonnes de
         * façon fiable avant Android 11, donc la table est reconstruite puis recopiée.
         * Les grammes deviennent des millilitres (densité de résine ≈ 1,1 g/cm³) et le
         * prix au kilo devient un prix au litre.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `print_jobs_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `projectId` INTEGER,
                        `fileName` TEXT NOT NULL,
                        `printer` TEXT NOT NULL,
                        `resinType` TEXT NOT NULL,
                        `resinColor` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `scalePercent` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `layerHeightMicrons` INTEGER NOT NULL,
                        `estimatedMinutes` INTEGER NOT NULL,
                        `actualMinutes` INTEGER NOT NULL,
                        `startedAt` INTEGER,
                        `finishedAt` INTEGER,
                        `scheduledAt` INTEGER,
                        `reminderAt` INTEGER,
                        `reminderEnabled` INTEGER NOT NULL,
                        `resinMl` REAL NOT NULL,
                        `resinPricePerLitre` REAL NOT NULL,
                        `extraCost` REAL NOT NULL,
                        `plateFullyOk` INTEGER,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`)
                            ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `print_jobs_new` (
                        id, name, projectId, fileName, printer, resinType, resinColor,
                        status, scalePercent, quantity, layerHeightMicrons,
                        estimatedMinutes, actualMinutes, startedAt, finishedAt, scheduledAt,
                        reminderAt, reminderEnabled, resinMl, resinPricePerLitre, extraCost,
                        plateFullyOk, notes, createdAt, updatedAt
                    )
                    SELECT
                        id, name, projectId, fileName, printer,
                        CASE
                            WHEN material IN ('PLA','PLA+','PETG','ABS','ASA','TPU','PA-CF','')
                                THEN 'Résine standard'
                            ELSE material
                        END,
                        filamentColor,
                        status, scalePercent, quantity, 50,
                        estimatedMinutes, actualMinutes, startedAt, finishedAt, scheduledAt,
                        reminderAt, reminderEnabled,
                        filamentGrams / 1.1, filamentPricePerKg * 1.1, extraCost,
                        plateFullyOk, notes, createdAt, updatedAt
                    FROM `print_jobs`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `print_jobs`")
                db.execSQL("ALTER TABLE `print_jobs_new` RENAME TO `print_jobs`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_print_jobs_projectId` ON `print_jobs` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_print_jobs_status` ON `print_jobs` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_print_jobs_scheduledAt` ON `print_jobs` (`scheduledAt`)")
            }
        }
    }
}
