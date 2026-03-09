package com.josski.simpleshortcut.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.josski.simpleshortcut.DeeplinkLauncher
import com.josski.simpleshortcut.R
import com.josski.simpleshortcut.data.ShortcutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShortcutWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SHORTCUT_CLICK) {
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

    companion object {
        const val ACTION_SHORTCUT_CLICK = "com.josski.simpleshortcut.ACTION_SHORTCUT_CLICK"
        const val EXTRA_SHORTCUT_ID = "extra_shortcut_id"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Wire RemoteViewsService for the list
            val serviceIntent = Intent(context, ShortcutWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            // Template PendingIntent for list item clicks
            val clickIntent = Intent(context, ShortcutWidgetProvider::class.java).apply {
                action = ACTION_SHORTCUT_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, clickPendingIntent)

            // "+ Add" button opens MainActivity
            val addIntent = Intent(context, com.josski.simpleshortcut.MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(com.josski.simpleshortcut.MainActivity.EXTRA_OPEN_ADD_DIALOG, true)
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, appWidgetId + 1000, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun notifyWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, ShortcutWidgetProvider::class.java)
            )
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
        }
    }
}