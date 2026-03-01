package com.josski.simpleshortcut.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.josski.simpleshortcut.R
import com.josski.simpleshortcut.data.Shortcut
import com.josski.simpleshortcut.data.ShortcutDatabase

class ShortcutRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var shortcuts: List<Shortcut> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val db = ShortcutDatabase.getDatabase(context)
        shortcuts = db.shortcutDao().getAllShortcutsSync()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = shortcuts.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= shortcuts.size) return RemoteViews(context.packageName, R.layout.widget_item)
        
        val item = shortcuts[position]
        return RemoteViews(context.packageName, R.layout.widget_item).apply {
            setTextViewText(R.id.item_emoji, item.emoji)
            setTextViewText(R.id.item_label, item.label)

            val fillInIntent = Intent().apply {
                putExtra(ShortcutWidgetProvider.EXTRA_SHORTCUT_ID, item.id)
            }
            setOnClickFillInIntent(R.id.item_root, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = shortcuts[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true
}