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

    @Query("SELECT DISTINCT category FROM shortcuts WHERE category != '' ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM shortcuts WHERE category = :category ORDER BY rank ASC, createdAt DESC")
    fun getByCategory(category: String): Flow<List<Shortcut>>

    @Query("SELECT * FROM shortcuts WHERE label LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%' OR deeplink LIKE '%' || :query || '%' ORDER BY rank ASC")
    fun search(query: String): Flow<List<Shortcut>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: Shortcut)

    @Delete
    suspend fun delete(shortcut: Shortcut)

    @Update
    suspend fun update(shortcut: Shortcut)

    @Update
    suspend fun updateAll(shortcuts: List<Shortcut>)

    @Query("UPDATE shortcuts SET tapCount = tapCount + 1, lastUsedAt = :now WHERE id = :id")
    suspend fun incrementTapCount(id: String, now: Long = System.currentTimeMillis())
}