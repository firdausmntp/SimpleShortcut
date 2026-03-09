package com.josski.simpleshortcut

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.josski.simpleshortcut.data.Shortcut
import com.josski.simpleshortcut.databinding.ItemShortcutBinding

class ShortcutAdapter(
    private val onClick: (Shortcut) -> Unit,
    private val onDelete: (Shortcut) -> Unit,
    private val onEdit: (Shortcut) -> Unit,
    private val onLongClick: ((Shortcut) -> Unit)? = null,
    private val onDragStart: ((RecyclerView.ViewHolder) -> Unit)? = null
) : ListAdapter<Shortcut, ShortcutAdapter.ShortcutViewHolder>(ShortcutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val binding = ItemShortcutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShortcutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShortcutViewHolder(private val binding: ItemShortcutBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(shortcut: Shortcut) {
            binding.tvEmoji.text = shortcut.emoji
            binding.tvLabel.text = shortcut.label
            binding.tvPackage.text = shortcut.packageName

            if (shortcut.category.isNotBlank()) {
                binding.tvCategory.text = shortcut.category
                binding.tvCategory.visibility = View.VISIBLE
            } else {
                binding.tvCategory.visibility = View.GONE
            }

            if (shortcut.tapCount > 0) {
                binding.tvTapCount.text = binding.root.context.getString(
                    R.string.tap_count_label, shortcut.tapCount
                )
                binding.tvTapCount.visibility = View.VISIBLE
            } else {
                binding.tvTapCount.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(shortcut) }
            binding.root.setOnLongClickListener {
                onLongClick?.invoke(shortcut)
                true
            }
            binding.btnDrag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onDragStart?.invoke(this@ShortcutViewHolder)
                }
                false
            }
            binding.btnEdit.setOnClickListener { onEdit(shortcut) }
            binding.btnDelete.setOnClickListener { onDelete(shortcut) }
        }
    }

    class ShortcutDiffCallback : DiffUtil.ItemCallback<Shortcut>() {
        override fun areItemsTheSame(oldItem: Shortcut, newItem: Shortcut): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Shortcut, newItem: Shortcut): Boolean {
            return oldItem == newItem
        }
    }
}