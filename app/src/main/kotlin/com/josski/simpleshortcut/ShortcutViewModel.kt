package com.josski.simpleshortcut

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.josski.simpleshortcut.data.Shortcut
import com.josski.simpleshortcut.data.ShortcutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShortcutViewModel(private val repository: ShortcutRepository) : ViewModel() {

    val allShortcuts: StateFlow<List<Shortcut>> = repository.allShortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: String get() = _selectedCategory.value

    enum class SortMode { RANK, NAME, MOST_USED, RECENTLY_USED, DATE_CREATED }
    private val _sortMode = MutableStateFlow(SortMode.RANK)
    val sortMode: SortMode get() = _sortMode.value

    val filteredShortcuts: StateFlow<List<Shortcut>> = combine(
        allShortcuts, _searchQuery, _selectedCategory, _sortMode
    ) { shortcuts, query, category, sort ->
        shortcuts.filter { s ->
            val matchesQuery = query.isBlank() ||
                s.label.contains(query, ignoreCase = true) ||
                s.packageName.contains(query, ignoreCase = true) ||
                s.deeplink.contains(query, ignoreCase = true)
            val matchesCategory = category.isBlank() || s.category == category
            matchesQuery && matchesCategory
        }.let { filtered ->
            when (sort) {
                SortMode.RANK -> filtered.sortedBy { it.rank }
                SortMode.NAME -> filtered.sortedBy { it.label.lowercase() }
                SortMode.MOST_USED -> filtered.sortedByDescending { it.tapCount }
                SortMode.RECENTLY_USED -> filtered.sortedByDescending { it.lastUsedAt }
                SortMode.DATE_CREATED -> filtered.sortedByDescending { it.createdAt }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(category: String) { _selectedCategory.value = category }
    fun setSortMode(mode: SortMode) { _sortMode.value = mode }

    fun insert(shortcut: Shortcut) = viewModelScope.launch {
        repository.insert(shortcut)
    }

    fun update(shortcut: Shortcut) = viewModelScope.launch {
        repository.update(shortcut)
    }

    fun delete(shortcut: Shortcut) = viewModelScope.launch {
        repository.delete(shortcut)
    }

    fun updateRanks(shortcuts: List<Shortcut>) = viewModelScope.launch {
        val ranked = shortcuts.mapIndexed { index, s -> s.copy(rank = index) }
        repository.updateAll(ranked)
    }

    suspend fun getById(id: String): Shortcut? {
        return repository.getById(id)
    }

    fun recordTap(shortcut: Shortcut) = viewModelScope.launch {
        repository.recordTap(shortcut.id)
    }

    fun duplicate(shortcut: Shortcut) = viewModelScope.launch {
        val copy = shortcut.copy(
            id = java.util.UUID.randomUUID().toString(),
            label = "${shortcut.label} (copy)",
            tapCount = 0,
            lastUsedAt = 0,
            createdAt = System.currentTimeMillis()
        )
        repository.insert(copy)
    }
}

class ShortcutViewModelFactory(private val repository: ShortcutRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShortcutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShortcutViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}