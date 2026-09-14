package com.organisator.print3d.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.organisator.print3d.data.dao.PrintJobDao
import com.organisator.print3d.data.dao.PrintPartDao
import com.organisator.print3d.data.dao.ProjectDao

@Database(
    entities = [Project::class, PrintJob::class, PrintPart::class],
    version = 1,
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
            ).build().also { instance = it }
        }
    }
}
