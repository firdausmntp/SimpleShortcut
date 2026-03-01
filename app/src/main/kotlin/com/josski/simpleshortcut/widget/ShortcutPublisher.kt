package com.josski.simpleshortcut.widget

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import com.josski.simpleshortcut.MainActivity
import com.josski.simpleshortcut.data.Shortcut

object ShortcutPublisher {
    fun publishDynamicShortcuts(context: Context, shortcuts: List<Shortcut>) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        val dynamicShortcuts = shortcuts.take(shortcutManager.maxShortcutCountPerActivity)
            .mapIndexed { index, item ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(MainActivity.EXTRA_SHORTCUT_ID, item.id)
                }
                ShortcutInfo.Builder(context, "shortcut_${item.id}")
                    .setShortLabel(item.label.take(10))
                    .setLongLabel(item.label.take(25))
                    // .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
                    .setIntent(intent)
                    .setRank(index)
                    .build()
            }

        shortcutManager.dynamicShortcuts = dynamicShortcuts
    }
}