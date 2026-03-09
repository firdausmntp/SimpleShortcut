package com.josski.simpleshortcut.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.josski.simpleshortcut.widget.ShortcutPublisher
import com.josski.simpleshortcut.widget.ShortcutWidgetProvider
import com.josski.simpleshortcut.widget.SingleShortcutWidgetProvider

class ShortcutRepository(private val shortcutDao: ShortcutDao, private val context: Context) {

    val allShortcuts: Flow<List<Shortcut>> = shortcutDao.getAllShortcuts()
    val allCategories: Flow<List<String>> = shortcutDao.getAllCategories()

    fun search(query: String): Flow<List<Shortcut>> = shortcutDao.search(query)
    fun getByCategory(category: String): Flow<List<Shortcut>> = shortcutDao.getByCategory(category)

    suspend fun insert(shortcut: Shortcut) {
        shortcutDao.insert(shortcut)
        notifyChanges()
    }

    suspend fun delete(shortcut: Shortcut) {
        shortcutDao.delete(shortcut)
        notifyChanges()
    }

    suspend fun update(shortcut: Shortcut) {
        shortcutDao.update(shortcut)
        notifyChanges()
    }

    suspend fun updateAll(shortcuts: List<Shortcut>) {
        shortcutDao.updateAll(shortcuts)
        notifyChanges()
    }

    suspend fun getById(id: String): Shortcut? {
        return shortcutDao.getById(id)
    }

    suspend fun recordTap(id: String) {
        shortcutDao.incrementTapCount(id)
        notifyChanges()
    }

    private suspend fun notifyChanges() {
        val shortcuts = shortcutDao.getAllShortcutsSync()
        try { ShortcutPublisher.publishDynamicShortcuts(context, shortcuts) } catch (_: Exception) {}
        try { ShortcutWidgetProvider.notifyWidgets(context) } catch (_: Exception) {}
        try { SingleShortcutWidgetProvider.notifyAll(context) } catch (_: Exception) {}
    }
}