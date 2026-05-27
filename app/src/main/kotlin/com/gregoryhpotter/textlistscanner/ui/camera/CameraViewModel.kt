package com.gregoryhpotter.textlistscanner.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.data.repository.SettingsRepository
import com.gregoryhpotter.textlistscanner.data.repository.WordListRepository
import com.gregoryhpotter.textlistscanner.feedback.AudioFeedbackTone
import com.gregoryhpotter.textlistscanner.ocr.OcrMatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val wordList: List<WordEntry> = emptyList(),
    val activeMatches: List<OcrMatch> = emptyList(),
    // Match settings
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    // Feedback settings
    val hapticEnabled: Boolean = true,
    val audioEnabled: Boolean = true,
    val audioTone: AudioFeedbackTone = AudioFeedbackTone.Beep,
    val visualEnabled: Boolean = true
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val wordListRepository: WordListRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        observeWordList()
        _uiState.value = _uiState.value.copy(
            caseSensitive = settingsRepository.caseSensitive,
            wholeWord     = settingsRepository.wholeWord,
            hapticEnabled = settingsRepository.hapticEnabled,
            audioEnabled  = settingsRepository.audioEnabled,
            visualEnabled = settingsRepository.visualEnabled,
            audioTone     = settingsRepository.audioTone
        )
    }

    private fun observeWordList() {
        viewModelScope.launch {
            wordListRepository.wordsFlow.collect { words ->
                // Immediately filter current matches against the new word list
                val currentMatches = _uiState.value.activeMatches
                val filteredMatches = currentMatches.filter { match ->
                    words.any { it.text == match.entry.text && it.enabled }
                }
                _uiState.value = _uiState.value.copy(
                    wordList = words,
                    activeMatches = filteredMatches
                )
            }
        }
    }

    fun onOcrResults(matches: List<OcrMatch>) {
        // We always update to ensure bounding box changes are reflected in the UI
        _uiState.value = _uiState.value.copy(activeMatches = matches)
    }

    // -------------------------------------------------------------------------
    // Match settings
    // -------------------------------------------------------------------------

    fun setCaseSensitive(value: Boolean) {
        settingsRepository.caseSensitive = value
        _uiState.value = _uiState.value.copy(caseSensitive = value)
    }

    fun setWholeWord(value: Boolean) {
        settingsRepository.wholeWord = value
        _uiState.value = _uiState.value.copy(wholeWord = value)
    }

    // -------------------------------------------------------------------------
    // Feedback settings
    // -------------------------------------------------------------------------

    fun setHapticEnabled(value: Boolean) {
        settingsRepository.hapticEnabled = value
        _uiState.value = _uiState.value.copy(hapticEnabled = value)
    }

    fun setAudioEnabled(value: Boolean) {
        settingsRepository.audioEnabled = value
        _uiState.value = _uiState.value.copy(audioEnabled = value)
    }

    fun setVisualEnabled(value: Boolean) {
        settingsRepository.visualEnabled = value
        _uiState.value = _uiState.value.copy(visualEnabled = value)
    }

    fun setAudioTone(tone: AudioFeedbackTone) {
        settingsRepository.audioTone = tone
        _uiState.value = _uiState.value.copy(audioTone = tone)
    }
}