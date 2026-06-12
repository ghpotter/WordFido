package com.gregoryhpotter.textlistscanner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WordEntryEntity::class, WordProfileEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordEntryDao(): WordEntryDao
    abstract fun wordProfileDao(): WordProfileDao
}
