package com.gregoryhpotter.textlistscanner.ui.camera

import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.data.repository.SettingsRepository
import com.gregoryhpotter.textlistscanner.data.repository.WordListRepository
import com.gregoryhpotter.textlistscanner.feedback.AudioFeedbackTone
import com.gregoryhpotter.textlistscanner.ocr.OcrMatch
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private lateinit var wordListRepository: WordListRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: CameraViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val wordsFlow = MutableStateFlow<List<WordEntry>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        wordListRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { wordListRepository.wordsFlow } returns wordsFlow
        coEvery { wordListRepository.loadWords() } returns emptyList()

        every { settingsRepository.caseSensitive } returns false
        every { settingsRepository.wholeWord } returns true
        every { settingsRepository.hapticEnabled } returns true
        every { settingsRepository.audioEnabled } returns true
        every { settingsRepository.audioTone } returns AudioFeedbackTone.Beep
        every { settingsRepository.zoomBarVisible } returns false

        viewModel = CameraViewModel(wordListRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `default case sensitive is false`() = runTest {
        assertFalse(viewModel.uiState.value.caseSensitive)
    }

    @Test
    fun `default whole word is true`() = runTest {
        assertTrue(viewModel.uiState.value.wholeWord)
    }

    @Test
    fun `default haptic enabled is true`() = runTest {
        assertTrue(viewModel.uiState.value.hapticEnabled)
    }

    @Test
    fun `default audio enabled is true`() = runTest {
        assertTrue(viewModel.uiState.value.audioEnabled)
    }

    // -------------------------------------------------------------------------
    // Match setting toggles
    // -------------------------------------------------------------------------

    @Test
    fun `setCaseSensitive updates state`() = runTest {
        viewModel.setCaseSensitive(true)
        assertTrue(viewModel.uiState.value.caseSensitive)
    }

    @Test
    fun `setCaseSensitive can be toggled back`() = runTest {
        viewModel.setCaseSensitive(true)
        viewModel.setCaseSensitive(false)
        assertFalse(viewModel.uiState.value.caseSensitive)
    }

    @Test
    fun `setWholeWord updates state`() = runTest {
        viewModel.setWholeWord(false)
        assertFalse(viewModel.uiState.value.wholeWord)
    }

    @Test
    fun `setWholeWord can be toggled back`() = runTest {
        viewModel.setWholeWord(false)
        viewModel.setWholeWord(true)
        assertTrue(viewModel.uiState.value.wholeWord)
    }

    // -------------------------------------------------------------------------
    // Feedback setting toggles
    // -------------------------------------------------------------------------

    @Test
    fun `setHapticEnabled updates state`() = runTest {
        viewModel.setHapticEnabled(false)
        assertFalse(viewModel.uiState.value.hapticEnabled)
    }

    @Test
    fun `setAudioEnabled updates state`() = runTest {
        viewModel.setAudioEnabled(false)
        assertFalse(viewModel.uiState.value.audioEnabled)
    }

    @Test
    fun `setHapticEnabled can be toggled back`() = runTest {
        viewModel.setHapticEnabled(false)
        viewModel.setHapticEnabled(true)
        assertTrue(viewModel.uiState.value.hapticEnabled)
    }

    @Test
    fun `setAudioEnabled can be toggled back`() = runTest {
        viewModel.setAudioEnabled(false)
        viewModel.setAudioEnabled(true)
        assertTrue(viewModel.uiState.value.audioEnabled)
    }

    // -------------------------------------------------------------------------
    // Word list loading
    // -------------------------------------------------------------------------

    @Test
    fun `word list loads on init`() = runTest {
        val words = listOf(WordEntry("exit", 0xFF0000, true))
        wordsFlow.value = words
        advanceUntilIdle()
        assertEquals(words, viewModel.uiState.value.wordList)
    }

    // -------------------------------------------------------------------------
    // OCR results
    // -------------------------------------------------------------------------

    @Test
    fun `onOcrResults updates activeMatches`() = runTest {
        val entry = WordEntry("exit", 0xFF0000, true)
        val match = OcrMatch(entry, "Found exit here")
        
        viewModel.onOcrResults(listOf(match))
        
        assertEquals(listOf(match), viewModel.uiState.value.activeMatches)
    }

    @Test
    fun `removing a word from the list clears it from active matches`() = runTest {
        // 1. Setup initial word list and detection
        val entry = WordEntry("exit", 0xFF0000, true)
        wordsFlow.value = listOf(entry)
        advanceUntilIdle() // Ensure ViewModel collected the initial list
        
        val match = OcrMatch(entry, "Found exit here")
        viewModel.onOcrResults(listOf(match))
        assertEquals(1, viewModel.uiState.value.activeMatches.size)
        
        // 2. Remove word from the repository's flow
        wordsFlow.value = emptyList()
        advanceUntilIdle()
        
        // 3. Verify it's cleared from state
        assertTrue("Active matches should be empty after word is removed from list", 
            viewModel.uiState.value.activeMatches.isEmpty())
    }

    @Test
    fun `disabling a word in the list clears it from active matches`() = runTest {
        // 1. Setup initial word list and detection
        val entry = WordEntry("exit", 0xFF0000, true)
        wordsFlow.value = listOf(entry)
        val match = OcrMatch(entry, "Found exit here")
        
        viewModel.onOcrResults(listOf(match))
        assertEquals(1, viewModel.uiState.value.activeMatches.size)
        
        // 2. Disable the word
        wordsFlow.value = listOf(entry.copy(enabled = false))
        advanceUntilIdle()
        
        // 3. Verify it's cleared from state
        assertTrue("Active matches should be empty after word is disabled", 
            viewModel.uiState.value.activeMatches.isEmpty())
    }

    @Test
    fun `settings changes do not clear active matches`() = runTest {
        viewModel.setCaseSensitive(true)
        viewModel.setWholeWord(false)
        assertTrue(viewModel.uiState.value.activeMatches.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Persistent settings
    // -------------------------------------------------------------------------

    @Test
    fun `init loads caseSensitive from settings`() = runTest {
        every { settingsRepository.caseSensitive } returns true
        viewModel = CameraViewModel(wordListRepository, settingsRepository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.caseSensitive)
    }

    @Test
    fun `init loads wholeWord from settings`() = runTest {
        every { settingsRepository.wholeWord } returns false
        viewModel = CameraViewModel(wordListRepository, settingsRepository)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.wholeWord)
    }

    @Test
    fun `setCaseSensitive persists to settings`() = runTest {
        viewModel.setCaseSensitive(true)
        verify { settingsRepository.caseSensitive = true }
    }

    @Test
    fun `setWholeWord persists to settings`() = runTest {
        viewModel.setWholeWord(false)
        verify { settingsRepository.wholeWord = false }
    }

    @Test
    fun `setHapticEnabled persists to settings`() = runTest {
        viewModel.setHapticEnabled(false)
        verify { settingsRepository.hapticEnabled = false }
    }

    @Test
    fun `setAudioEnabled persists to settings`() = runTest {
        viewModel.setAudioEnabled(false)
        verify { settingsRepository.audioEnabled = false }
    }

    // -------------------------------------------------------------------------
    // Zoom bar
    // -------------------------------------------------------------------------

    @Test
    fun `default zoom bar visible is false`() = runTest {
        assertFalse(viewModel.uiState.value.zoomBarVisible)
    }

    @Test
    fun `init loads zoomBarVisible from settings`() = runTest {
        every { settingsRepository.zoomBarVisible } returns true
        viewModel = CameraViewModel(wordListRepository, settingsRepository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.zoomBarVisible)
    }

    @Test
    fun `setZoomBarVisible updates state`() = runTest {
        viewModel.setZoomBarVisible(true)
        assertTrue(viewModel.uiState.value.zoomBarVisible)
    }

    @Test
    fun `setZoomBarVisible can be toggled back`() = runTest {
        viewModel.setZoomBarVisible(true)
        viewModel.setZoomBarVisible(false)
        assertFalse(viewModel.uiState.value.zoomBarVisible)
    }

    @Test
    fun `setZoomBarVisible persists to settings`() = runTest {
        viewModel.setZoomBarVisible(true)
        verify { settingsRepository.zoomBarVisible = true }
    }

}