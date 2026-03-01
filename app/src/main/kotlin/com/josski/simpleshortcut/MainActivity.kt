package com.josski.simpleshortcut

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.josski.simpleshortcut.data.Shortcut
import com.josski.simpleshortcut.databinding.ActivityMainBinding
import com.josski.simpleshortcut.databinding.DialogAddShortcutBinding
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    private val viewModel: ShortcutViewModel by viewModels {
        ShortcutViewModelFactory((application as SimpleShortcutApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ShortcutAdapter(
            onClick = { shortcut ->
                DeeplinkLauncher.launch(this, shortcut.packageName, shortcut.deeplink)
            },
            onDelete = { shortcut ->
                viewModel.delete(shortcut)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            showAddShortcutDialog()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allShortcuts.collect { shortcuts ->
                    adapter.submitList(shortcuts)
                }
            }
        }

        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: android.content.Intent) {
        // Open add dialog directly (e.g. from widget "+" button)
        if (intent.getBooleanExtra(EXTRA_OPEN_ADD_DIALOG, false)) {
            showAddShortcutDialog()
            return
        }
        // Launch shortcut via dynamic shortcut or widget tap
        val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID) ?: return
        lifecycleScope.launch {
            val shortcut = viewModel.getById(shortcutId)
            if (shortcut != null) {
                DeeplinkLauncher.launch(this@MainActivity, shortcut.packageName, shortcut.deeplink)
                finish()
            }
        }
    }

    private fun showAddShortcutDialog() {
        val dialogBinding = DialogAddShortcutBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_add_shortcut)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val emoji = dialogBinding.etEmoji.text.toString().trim()
                val label = dialogBinding.etLabel.text.toString().trim()
                val packageName = dialogBinding.etPackageName.text.toString().trim()
                val deeplink = dialogBinding.etDeeplink.text.toString().trim()

                if (label.isNotEmpty() && (packageName.isNotEmpty() || deeplink.isNotEmpty())) {
                    val shortcut = Shortcut(
                        id = UUID.randomUUID().toString(),
                        label = label,
                        packageName = packageName,
                        deeplink = deeplink,
                        emoji = emoji.ifEmpty { "⚡" }
                    )
                    viewModel.insert(shortcut)
                } else {
                    Toast.makeText(this, "Label wajib diisi, lengkapi juga Package Name atau Deeplink", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
    
    companion object {
        const val EXTRA_SHORTCUT_ID = "extra_shortcut_id"
        const val EXTRA_OPEN_ADD_DIALOG = "extra_open_add_dialog"
    }
}