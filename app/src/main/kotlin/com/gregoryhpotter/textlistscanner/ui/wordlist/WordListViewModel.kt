package com.gregoryhpotter.textlistscanner.ui.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.data.model.WordProfile
import com.gregoryhpotter.textlistscanner.data.repository.WordListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordListUiState(
    val words: List<WordEntry> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val profiles: List<WordProfile> = emptyList(),
    val activeProfileId: Long = -1L,
    val pendingDelete: WordEntry? = null
)

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val repository: WordListRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(WordListUiState())
    val uiState: StateFlow<WordListUiState> = _uiState.asStateFlow()

    private var allWords: List<WordEntry> = emptyList()

    init {
        viewModelScope.launch { repository.ensureDefaultProfile() }

        viewModelScope.launch {
            combine(repository.wordsFlow, _searchQuery) { words, query ->
                allWords = words
                if (query.isBlank()) words
                else words.filter { it.text.contains(query, ignoreCase = true) }
            }.collect { filtered ->
                _uiState.value = _uiState.value.copy(words = filtered, isLoading = false)
            }
        }

        viewModelScope.launch {
            repository.profilesFlow.collect { profiles ->
                _uiState.value = _uiState.value.copy(
                    profiles = profiles,
                    activeProfileId = repository.activeProfileId
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // -------------------------------------------------------------------------
    // Word management
    // -------------------------------------------------------------------------

    fun addWord(text: String, color: Int) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.addWord(WordEntry(text = trimmed, color = color, enabled = true))
        }
    }

    fun removeWord(entry: WordEntry) {
        _uiState.value = _uiState.value.copy(pendingDelete = entry)
        viewModelScope.launch { repository.removeWord(entry.text) }
    }

    fun undoDelete() {
        val entry = _uiState.value.pendingDelete ?: return
        _uiState.value = _uiState.value.copy(pendingDelete = null)
        viewModelScope.launch { repository.addWord(entry) }
    }

    fun confirmDelete() {
        _uiState.value = _uiState.value.copy(pendingDelete = null)
    }

    fun toggleWord(text: String) {
        val entry = _uiState.value.words.find { it.text == text } ?: return
        viewModelScope.launch { repository.setWordEnabled(text, !entry.enabled) }
    }

    fun updateColor(text: String, color: Int) {
        viewModelScope.launch { repository.updateColor(text, color) }
    }

    fun importFromText(raw: String) {
        if (raw.isBlank()) return
        viewModelScope.launch {
            repository.saveWords(repository.importFromText(raw))
        }
    }

    // -------------------------------------------------------------------------
    // Profile management
    // -------------------------------------------------------------------------

    fun switchProfile(id: Long) {
        repository.setActiveProfile(id)
        _uiState.value = _uiState.value.copy(activeProfileId = id)
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            val id = repository.createProfile(name)
            repository.setActiveProfile(id)
            _uiState.value = _uiState.value.copy(activeProfileId = id)
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteProfile(id)
            _uiState.value = _uiState.value.copy(activeProfileId = repository.activeProfileId)
        }
    }

    fun renameProfile(id: Long, name: String) {
        viewModelScope.launch { repository.renameProfile(id, name) }
    }

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    fun buildExportText(): String = allWords.joinToString("\n") { it.text }
}
