package com.josski.simpleshortcut.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.josski.simpleshortcut.widget.ShortcutPublisher
import com.josski.simpleshortcut.widget.ShortcutWidgetProvider
import com.josski.simpleshortcut.widget.SingleShortcutWidgetProvider

class ShortcutRepository(private val shortcutDao: ShortcutDao, private val context: Context) {

    val allShortcuts: Flow<List<Shortcut>> = shortcutDao.getAllShortcuts()

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

    suspend fun getById(id: String): Shortcut? {
        return shortcutDao.getById(id)
    }

    private suspend fun notifyChanges() {
        val shortcuts = shortcutDao.getAllShortcutsSync()
        ShortcutPublisher.publishDynamicShortcuts(context, shortcuts)
        ShortcutWidgetProvider.notifyWidgets(context)
        SingleShortcutWidgetProvider.notifyAll(context)
    }
}