package com.gregoryhpotter.textlistscanner.ui.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.data.repository.WordListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordListUiState(
    val words: List<WordEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val repository: WordListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordListUiState())
    val uiState: StateFlow<WordListUiState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    private fun loadWords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val words = repository.loadWords()
                _uiState.value = _uiState.value.copy(words = words, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load word list"
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Add
    // -------------------------------------------------------------------------

    fun addWord(text: String, color: Int) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            repository.addWord(WordEntry(text = trimmed, color = color, enabled = true))
            reload()
        }
    }

    // -------------------------------------------------------------------------
    // Remove
    // -------------------------------------------------------------------------

    fun removeWord(text: String) {
        viewModelScope.launch {
            repository.removeWord(text)
            reload()
        }
    }

    // -------------------------------------------------------------------------
    // Toggle enabled
    // -------------------------------------------------------------------------

    fun toggleWord(text: String) {
        val entry = _uiState.value.words.find { it.text == text } ?: return
        viewModelScope.launch {
            repository.setWordEnabled(text, !entry.enabled)
            reload()
        }
    }

    // -------------------------------------------------------------------------
    // Update color
    // -------------------------------------------------------------------------

    fun updateColor(text: String, color: Int) {
        viewModelScope.launch {
            repository.updateColor(text, color)
            reload()
        }
    }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    fun importFromText(raw: String) {
        if (raw.isBlank()) return
        viewModelScope.launch {
            val imported = repository.importFromText(raw)
            repository.saveWords(imported)
            reload()
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private suspend fun reload() {
        val words = repository.loadWords()
        _uiState.value = _uiState.value.copy(words = words, isLoading = false)
    }
}