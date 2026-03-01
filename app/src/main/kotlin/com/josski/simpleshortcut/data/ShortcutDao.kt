package com.josski.simpleshortcut.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {

    @Query("SELECT * FROM shortcuts ORDER BY rank ASC, createdAt DESC")
    fun getAllShortcuts(): Flow<List<Shortcut>>

    @Query("SELECT * FROM shortcuts ORDER BY rank ASC, createdAt DESC")
    fun getAllShortcutsSync(): List<Shortcut>

    @Query("SELECT * FROM shortcuts WHERE id = :id")
    suspend fun getById(id: String): Shortcut?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: Shortcut)

    @Delete
    suspend fun delete(shortcut: Shortcut)

    @Update
    suspend fun update(shortcut: Shortcut)
}