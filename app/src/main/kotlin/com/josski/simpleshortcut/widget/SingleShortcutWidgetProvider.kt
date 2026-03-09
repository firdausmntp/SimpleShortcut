package com.josski.simpleshortcut.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.josski.simpleshortcut.DeeplinkLauncher
import com.josski.simpleshortcut.R
import com.josski.simpleshortcut.data.ShortcutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 1×1 single icon widget. Each instance is mapped to one Shortcut record
 * via SharedPreferences (key = "widget_<id>").
 */
class SingleShortcutWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        appWidgetIds.forEach { prefs.edit().remove(prefKey(it)).apply() }
    }

    companion object {
        private const val PREFS_NAME = "single_widget_prefs"
        private const val ACTION_SINGLE_CLICK = "com.josski.simpleshortcut.ACTION_SINGLE_CLICK"
        private const val EXTRA_SHORTCUT_ID = "extra_single_shortcut_id"

        fun prefKey(widgetId: Int) = "widget_$widgetId"

        fun saveShortcutForWidget(context: Context, widgetId: Int, shortcutId: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(prefKey(widgetId), shortcutId).apply()
        }

        fun getShortcutIdForWidget(context: Context, widgetId: Int): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(prefKey(widgetId), null)
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val shortcutId = getShortcutIdForWidget(context, widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_single)

            if (shortcutId == null) {
                // Not yet configured
                views.setTextViewText(R.id.single_emoji, "⚡")
                views.setTextViewText(R.id.single_label, "Tap to setup")
                manager.updateAppWidget(widgetId, views)
                return
            }

            // Load shortcut on background thread and update
            CoroutineScope(Dispatchers.IO).launch {
                val db = ShortcutDatabase.getDatabase(context)
                val shortcut = db.shortcutDao().getById(shortcutId)

                val tapIntent = Intent(context, SingleShortcutWidgetProvider::class.java).apply {
                    action = ACTION_SINGLE_CLICK
                    putExtra(EXTRA_SHORTCUT_ID, shortcutId)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                val tapPending = PendingIntent.getBroadcast(
                    context, widgetId, tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                views.setTextViewText(R.id.single_emoji, shortcut?.emoji ?: "⚡")
                views.setTextViewText(R.id.single_label, shortcut?.label ?: "—")
                views.setOnClickPendingIntent(R.id.single_emoji, tapPending)
                views.setOnClickPendingIntent(R.id.single_label, tapPending)

                manager.updateAppWidget(widgetId, views)
            }
        }

        fun notifyAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, SingleShortcutWidgetProvider::class.java)
            )
            for (id in ids) updateWidget(context, manager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SINGLE_CLICK) {
            val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID) ?: return
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = ShortcutDatabase.getDatabase(context)
                    val item = db.shortcutDao().getById(shortcutId)
                    if (item != null) {
                        DeeplinkLauncher.launch(context, item.packageName, item.deeplink)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}