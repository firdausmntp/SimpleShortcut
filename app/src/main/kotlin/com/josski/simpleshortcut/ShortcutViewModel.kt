package com.josski.simpleshortcut

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.josski.simpleshortcut.data.Shortcut
import com.josski.simpleshortcut.data.ShortcutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShortcutViewModel(private val repository: ShortcutRepository) : ViewModel() {

    val allShortcuts: StateFlow<List<Shortcut>> = repository.allShortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(shortcut: Shortcut) = viewModelScope.launch {
        repository.insert(shortcut)
    }

    fun update(shortcut: Shortcut) = viewModelScope.launch {
        repository.update(shortcut)
    }

    fun delete(shortcut: Shortcut) = viewModelScope.launch {
        repository.delete(shortcut)
    }

    suspend fun getById(id: String): Shortcut? {
        return repository.getById(id)
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