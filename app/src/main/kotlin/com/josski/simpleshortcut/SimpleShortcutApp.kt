package com.josski.simpleshortcut

import android.app.Application
import com.josski.simpleshortcut.data.ShortcutDatabase
import com.josski.simpleshortcut.data.ShortcutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SimpleShortcutApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { ShortcutDatabase.getDatabase(this) }
    val repository by lazy { ShortcutRepository(database.shortcutDao(), this) }

    override fun onCreate() {
        super.onCreate()
        // Re-publish shortcuts on cold start
        applicationScope.launch {
            try {
                val shortcuts = database.shortcutDao().getAllShortcutsSync()
                com.josski.simpleshortcut.widget.ShortcutPublisher.publishDynamicShortcuts(this@SimpleShortcutApp, shortcuts)
            } catch (_: Exception) {
                // ShortcutManager rate-limit or other failures should not crash the app
            }
        }
    }
}