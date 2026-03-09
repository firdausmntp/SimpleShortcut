package com.josski.simpleshortcut.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Shortcut::class], version = 2, exportSchema = false)
abstract class ShortcutDatabase : RoomDatabase() {

    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: ShortcutDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shortcuts ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shortcuts ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): ShortcutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShortcutDatabase::class.java,
                    "shortcut_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}