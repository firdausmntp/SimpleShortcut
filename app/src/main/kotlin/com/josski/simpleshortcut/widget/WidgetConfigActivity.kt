package com.josski.simpleshortcut.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.josski.simpleshortcut.R
import com.josski.simpleshortcut.data.Shortcut
import com.josski.simpleshortcut.data.ShortcutDatabase
import com.josski.simpleshortcut.databinding.ActivityWidgetConfigBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Configuration Activity for the 1×1 single-icon widget.
 * Shown automatically by the launcher when the user adds the widget.
 * The user picks which shortcut to assign to this widget instance.
 */
class WidgetConfigActivity : Activity() {

    private lateinit var binding: ActivityWidgetConfigBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result = CANCELED (user backs out without picking)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = ShortcutDatabase.getDatabase(this)

        CoroutineScope(Dispatchers.IO).launch {
            val shortcuts = db.shortcutDao().getAllShortcutsSync()
            withContext(Dispatchers.Main) {
                if (shortcuts.isEmpty()) {
                    // No shortcuts exist — tell user to add some first, then cancel
                    android.widget.Toast.makeText(
                        this@WidgetConfigActivity,
                        getString(R.string.widget_config_empty),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@withContext
                }
                binding.configRecycler.layoutManager = LinearLayoutManager(this@WidgetConfigActivity)
                binding.configRecycler.adapter = ShortcutPickerAdapter(shortcuts) { selected ->
                    onShortcutPicked(selected)
                }
            }
        }
    }

    private fun onShortcutPicked(shortcut: Shortcut) {
        // Save mapping: widgetId → shortcutId
        SingleShortcutWidgetProvider.saveShortcutForWidget(this, appWidgetId, shortcut.id)

        // Trigger initial widget update
        val manager = AppWidgetManager.getInstance(this)
        SingleShortcutWidgetProvider.updateWidget(this, manager, appWidgetId)

        // Return OK to the launcher
        val result = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    // ---- Inner adapter ----

    inner class ShortcutPickerAdapter(
        private val items: List<Shortcut>,
        private val onPick: (Shortcut) -> Unit
    ) : RecyclerView.Adapter<ShortcutPickerAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val emoji: TextView = view.findViewById(R.id.picker_emoji)
            val label: TextView = view.findViewById(R.id.picker_label)
            val pkg: TextView = view.findViewById(R.id.picker_package)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shortcut_picker, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.emoji.text = item.emoji
            holder.label.text = item.label
            holder.pkg.text = item.packageName.ifBlank { item.deeplink }
            holder.itemView.setOnClickListener { onPick(item) }
        }
    }
}