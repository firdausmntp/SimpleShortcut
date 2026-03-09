package com.josski.simpleshortcut

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.josski.simpleshortcut.data.Shortcut
import com.josski.simpleshortcut.databinding.ActivityMainBinding
import com.josski.simpleshortcut.databinding.DialogAddShortcutBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ShortcutAdapter
    private lateinit var touchHelper: ItemTouchHelper
    private var reorderList = mutableListOf<Shortcut>()
    private var isDragging = false

    private val viewModel: ShortcutViewModel by viewModels {
        ShortcutViewModelFactory((application as SimpleShortcutApp).repository)
    }

    // SAF file pickers for import/export
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { exportToUri(it) } }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFromUri(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ShortcutAdapter(
            onClick = { shortcut ->
                viewModel.recordTap(shortcut)
                DeeplinkLauncher.launch(this, shortcut.packageName, shortcut.deeplink)
            },
            onDelete = { shortcut ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Hapus \"${shortcut.label}\"?")
                    .setMessage("Shortcut ini akan dihapus permanen.")
                    .setPositiveButton(R.string.btn_delete) { _, _ -> viewModel.delete(shortcut) }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            },
            onEdit = { shortcut ->
                showEditShortcutDialog(shortcut)
            },
            onLongClick = { shortcut ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(shortcut.label)
                    .setItems(arrayOf(
                        getString(R.string.menu_pin),
                        getString(R.string.action_edit_shortcut),
                        getString(R.string.menu_duplicate),
                        getString(R.string.btn_delete)
                    )) { _, which ->
                        when (which) {
                            0 -> pinShortcut(shortcut)
                            1 -> showEditShortcutDialog(shortcut)
                            2 -> viewModel.duplicate(shortcut)
                            3 -> viewModel.delete(shortcut)
                        }
                    }
                    .show()
            },
            onDragStart = { viewHolder ->
                touchHelper.startDrag(viewHolder)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Drag-to-reorder
        touchHelper = ItemTouchHelper(ShortcutItemTouchHelper(
            onMoved = { from, to ->
                if (from in reorderList.indices && to in reorderList.indices) {
                    Collections.swap(reorderList, from, to)
                    adapter.notifyItemMoved(from, to)
                }
            },
            onDragStateChanged = { dragging -> isDragging = dragging },
            onDropped = {
                if (reorderList.isNotEmpty()) {
                    viewModel.updateRanks(reorderList.toList())
                }
            }
        ))
        touchHelper.attachToRecyclerView(binding.recyclerView)

        binding.fabAdd.setOnClickListener {
            showAddShortcutDialog()
        }

        // Search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observe filtered shortcuts (skip during drag to avoid resetting reorder)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredShortcuts.collect { shortcuts ->
                    if (!isDragging) {
                        reorderList = shortcuts.toMutableList()
                        adapter.submitList(shortcuts)
                    }
                    supportActionBar?.subtitle = getString(R.string.shortcut_count, shortcuts.size)
                }
            }
        }

        // Observe categories for chip group
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategories.collect { categories ->
                    updateCategoryChips(categories)
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_SORT, 0, R.string.menu_sort)
        menu.add(0, MENU_EXPORT, 1, R.string.menu_export)
        menu.add(0, MENU_IMPORT, 2, R.string.menu_import)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_SORT -> {
                showSortDialog()
                true
            }
            MENU_EXPORT -> {
                exportLauncher.launch("simpleshortcut_backup.json")
                true
            }
            MENU_IMPORT -> {
                importLauncher.launch(arrayOf("application/json"))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortDialog() {
        val sortNames = arrayOf(
            getString(R.string.sort_rank),
            getString(R.string.sort_name),
            getString(R.string.sort_most_used),
            getString(R.string.sort_recently_used),
            getString(R.string.sort_date_created)
        )
        val currentIndex = ShortcutViewModel.SortMode.entries.indexOf(viewModel.sortMode)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_sort)
            .setSingleChoiceItems(sortNames, currentIndex) { dialog, which ->
                viewModel.setSortMode(ShortcutViewModel.SortMode.entries[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun handleIntent(intent: Intent) {
        if (isFinishing || isDestroyed) return

        if (intent.getBooleanExtra(EXTRA_OPEN_ADD_DIALOG, false)) {
            intent.removeExtra(EXTRA_OPEN_ADD_DIALOG)
            showAddShortcutDialog()
            return
        }
        val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID) ?: return
        lifecycleScope.launch {
            val shortcut = viewModel.getById(shortcutId)
            if (shortcut != null) {
                DeeplinkLauncher.launch(this@MainActivity, shortcut.packageName, shortcut.deeplink)
                finish()
            }
        }
    }

    private fun updateCategoryChips(categories: List<String>) {
        val chipGroup = binding.chipGroupCategories
        chipGroup.removeAllViews()
        val selected = viewModel.selectedCategory

        // "All" chip
        val allChip = Chip(this).apply {
            text = getString(R.string.all_categories)
            isCheckable = true
            isChecked = selected.isBlank()
            setOnClickListener {
                viewModel.setSelectedCategory("")
            }
        }
        chipGroup.addView(allChip)

        for (cat in categories) {
            val chip = Chip(this).apply {
                text = cat
                isCheckable = true
                isChecked = cat == selected
                setOnClickListener {
                    viewModel.setSelectedCategory(cat)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun showAddShortcutDialog() {
        if (isFinishing || isDestroyed) return
        val dialogBinding = DialogAddShortcutBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_add_shortcut)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null) // set below to prevent auto-dismiss
            .setNegativeButton(R.string.btn_cancel, null)
            .setNeutralButton(R.string.btn_test, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val emoji = dialogBinding.etEmoji.text.toString().trim()
                val label = dialogBinding.etLabel.text.toString().trim()
                val packageName = dialogBinding.etPackageName.text.toString().trim()
                val deeplink = dialogBinding.etDeeplink.text.toString().trim()
                val category = dialogBinding.etCategory.text.toString().trim()

                if (label.isNotEmpty() && (packageName.isNotEmpty() || deeplink.isNotEmpty())) {
                    val shortcut = Shortcut(
                        id = UUID.randomUUID().toString(),
                        label = label,
                        packageName = packageName,
                        deeplink = deeplink,
                        emoji = emoji.ifEmpty { "⚡" },
                        category = category
                    )
                    viewModel.insert(shortcut)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Label wajib diisi, lengkapi juga Package Name atau Deeplink", Toast.LENGTH_SHORT).show()
                }
            }

            // Test-fire: try launch without saving
            dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                val pkg = dialogBinding.etPackageName.text.toString().trim()
                val link = dialogBinding.etDeeplink.text.toString().trim()
                if (pkg.isNotEmpty() || link.isNotEmpty()) {
                    DeeplinkLauncher.launch(this, pkg, link)
                } else {
                    Toast.makeText(this, "Isi Package Name atau Deeplink dulu untuk test", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun showEditShortcutDialog(shortcut: Shortcut) {
        if (isFinishing || isDestroyed) return
        val dialogBinding = DialogAddShortcutBinding.inflate(layoutInflater)

        // Pre-fill
        dialogBinding.etEmoji.setText(shortcut.emoji)
        dialogBinding.etLabel.setText(shortcut.label)
        dialogBinding.etPackageName.setText(shortcut.packageName)
        dialogBinding.etDeeplink.setText(shortcut.deeplink)
        dialogBinding.etCategory.setText(shortcut.category)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_edit_shortcut)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .setNeutralButton(R.string.btn_test, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val emoji = dialogBinding.etEmoji.text.toString().trim()
                val label = dialogBinding.etLabel.text.toString().trim()
                val packageName = dialogBinding.etPackageName.text.toString().trim()
                val deeplink = dialogBinding.etDeeplink.text.toString().trim()
                val category = dialogBinding.etCategory.text.toString().trim()

                if (label.isNotEmpty() && (packageName.isNotEmpty() || deeplink.isNotEmpty())) {
                    viewModel.update(
                        shortcut.copy(
                            label = label,
                            packageName = packageName,
                            deeplink = deeplink,
                            emoji = emoji.ifEmpty { "⚡" },
                            category = category
                        )
                    )
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Label wajib diisi, lengkapi juga Package Name atau Deeplink", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                val pkg = dialogBinding.etPackageName.text.toString().trim()
                val link = dialogBinding.etDeeplink.text.toString().trim()
                if (pkg.isNotEmpty() || link.isNotEmpty()) {
                    DeeplinkLauncher.launch(this, pkg, link)
                } else {
                    Toast.makeText(this, "Isi Package Name atau Deeplink dulu untuk test", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    // ---- Pin Shortcut ----

    fun pinShortcut(shortcut: Shortcut) {
        val sm = getSystemService(ShortcutManager::class.java)
        if (sm.isRequestPinShortcutSupported) {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                putExtra(EXTRA_SHORTCUT_ID, shortcut.id)
            }
            val pinInfo = ShortcutInfo.Builder(this, "pin_${shortcut.id}")
                .setShortLabel(shortcut.label)
                .setLongLabel("${shortcut.emoji} ${shortcut.label}")
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()
            sm.requestPinShortcut(pinInfo, null)
            Toast.makeText(this, R.string.msg_pinned, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.msg_pin_not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Import / Export JSON ----

    private fun exportToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val shortcuts = viewModel.allShortcuts.value
                val jsonArray = JSONArray()
                for (s in shortcuts) {
                    jsonArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("label", s.label)
                        put("packageName", s.packageName)
                        put("deeplink", s.deeplink)
                        put("emoji", s.emoji)
                        put("rank", s.rank)
                        put("category", s.category)
                        put("isPinned", s.isPinned)
                        put("tapCount", s.tapCount)
                        put("lastUsedAt", s.lastUsedAt)
                    })
                }
                contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(jsonArray.toString(2).toByteArray())
                }
                Toast.makeText(this@MainActivity, R.string.msg_exported, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Export gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw Exception("Tidak bisa baca file")
                val arr = JSONArray(json)
                var count = 0
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val shortcut = Shortcut(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        label = obj.getString("label"),
                        packageName = obj.optString("packageName", ""),
                        deeplink = obj.optString("deeplink", ""),
                        emoji = obj.optString("emoji", "⚡"),
                        rank = obj.optInt("rank", 0),
                        category = obj.optString("category", ""),
                        isPinned = obj.optBoolean("isPinned", false),
                        tapCount = obj.optInt("tapCount", 0),
                        lastUsedAt = obj.optLong("lastUsedAt", 0)
                    )
                    viewModel.insert(shortcut)
                    count++
                }
                Toast.makeText(this@MainActivity, getString(R.string.msg_imported, count), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, getString(R.string.msg_import_failed, e.message ?: "unknown"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_SHORTCUT_ID = "extra_shortcut_id"
        const val EXTRA_OPEN_ADD_DIALOG = "extra_open_add_dialog"
        private const val MENU_SORT = 99
        private const val MENU_EXPORT = 100
        private const val MENU_IMPORT = 101
    }
}