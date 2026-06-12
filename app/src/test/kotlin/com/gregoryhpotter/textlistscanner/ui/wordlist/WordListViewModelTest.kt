package com.gregoryhpotter.textlistscanner.ui.wordlist

import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.data.model.WordProfile
import com.gregoryhpotter.textlistscanner.data.repository.WordListRepository
import io.mockk.coVerify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordListViewModelTest {

    private lateinit var repository: WordListRepository
    private lateinit var viewModel: WordListViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val wordsFlow = MutableStateFlow<List<WordEntry>>(emptyList())
    private val profilesFlow = MutableStateFlow<List<WordProfile>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.wordsFlow } returns wordsFlow
        every { repository.profilesFlow } returns profilesFlow
        every { repository.activeProfileId } returns -1L
        viewModel = WordListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Words flow observation
    // -------------------------------------------------------------------------

    @Test
    fun `words from flow appear in ui state`() = runTest {
        val words = listOf(WordEntry("exit", 0xFF0000, true))
        wordsFlow.value = words
        advanceUntilIdle()

        assertEquals(words, viewModel.uiState.value.words)
    }

    @Test
    fun `ui state starts with empty list`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.words.isEmpty())
    }

    @Test
    fun `isLoading becomes false after flow emits`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @Test
    fun `search filters words by text`() = runTest {
        wordsFlow.value = listOf(
            WordEntry("exit", 0xFF0000, true),
            WordEntry("door", 0x00FF00, true)
        )
        advanceUntilIdle()

        viewModel.setSearchQuery("ex")
        advanceUntilIdle()

        assertEquals(listOf("exit"), viewModel.uiState.value.words.map { it.text })
    }

    @Test
    fun `empty search query shows all words`() = runTest {
        wordsFlow.value = listOf(
            WordEntry("exit", 0xFF0000, true),
            WordEntry("door", 0x00FF00, true)
        )
        advanceUntilIdle()

        viewModel.setSearchQuery("ex")
        viewModel.setSearchQuery("")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.words.size)
    }

    @Test
    fun `search is case-insensitive`() = runTest {
        wordsFlow.value = listOf(WordEntry("EXIT", 0xFF0000, true))
        advanceUntilIdle()

        viewModel.setSearchQuery("exit")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.words.size)
    }

    // -------------------------------------------------------------------------
    // Add word
    // -------------------------------------------------------------------------

    @Test
    fun `addWord calls repository with correct entry`() = runTest {
        viewModel.addWord("exit", 0xFF0000)
        advanceUntilIdle()

        coVerify { repository.addWord(match { it.text == "exit" && it.color == 0xFF0000 }) }
    }

    @Test
    fun `addWord trims whitespace`() = runTest {
        viewModel.addWord("  exit  ", 0xFF0000)
        advanceUntilIdle()

        coVerify { repository.addWord(match { it.text == "exit" }) }
    }

    @Test
    fun `addWord with blank text does nothing`() = runTest {
        viewModel.addWord("   ", 0xFF0000)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.addWord(any()) }
    }

    @Test
    fun `addWord sets enabled to true by default`() = runTest {
        viewModel.addWord("exit", 0xFF0000)
        advanceUntilIdle()

        coVerify { repository.addWord(match { it.enabled }) }
    }

    // -------------------------------------------------------------------------
    // Remove word + undo
    // -------------------------------------------------------------------------

    @Test
    fun `removeWord calls repository`() = runTest {
        val entry = WordEntry("exit", 0xFF0000, true)
        viewModel.removeWord(entry)
        advanceUntilIdle()

        coVerify { repository.removeWord("exit") }
    }

    @Test
    fun `removeWord sets pendingDelete in ui state`() = runTest {
        val entry = WordEntry("exit", 0xFF0000, true)
        viewModel.removeWord(entry)

        assertEquals(entry, viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun `undoDelete re-adds the word`() = runTest {
        val entry = WordEntry("exit", 0xFF0000, true)
        viewModel.removeWord(entry)
        viewModel.undoDelete()
        advanceUntilIdle()

        coVerify { repository.addWord(entry) }
    }

    @Test
    fun `undoDelete clears pendingDelete`() = runTest {
        val entry = WordEntry("exit", 0xFF0000, true)
        viewModel.removeWord(entry)
        viewModel.undoDelete()

        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun `confirmDelete clears pendingDelete without re-adding`() = runTest {
        val entry = WordEntry("exit", 0xFF0000, true)
        viewModel.removeWord(entry)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingDelete)
        coVerify(exactly = 0) { repository.addWord(any()) }
    }

    // -------------------------------------------------------------------------
    // Toggle enabled
    // -------------------------------------------------------------------------

    @Test
    fun `toggleWord enables a disabled word`() = runTest {
        wordsFlow.value = listOf(WordEntry("exit", 0xFF0000, enabled = false))
        advanceUntilIdle()

        viewModel.toggleWord("exit")
        advanceUntilIdle()

        coVerify { repository.setWordEnabled("exit", true) }
    }

    @Test
    fun `toggleWord disables an enabled word`() = runTest {
        wordsFlow.value = listOf(WordEntry("exit", 0xFF0000, enabled = true))
        advanceUntilIdle()

        viewModel.toggleWord("exit")
        advanceUntilIdle()

        coVerify { repository.setWordEnabled("exit", false) }
    }

    @Test
    fun `toggleWord on unknown word does nothing`() = runTest {
        advanceUntilIdle()

        viewModel.toggleWord("unknown")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.setWordEnabled(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // Import from text
    // -------------------------------------------------------------------------

    @Test
    fun `importFromText calls repository import and saves result`() = runTest {
        val imported = listOf(WordEntry("exit", 0xFF0000, true))
        every { repository.importFromText(any()) } returns imported

        viewModel.importFromText("exit")
        advanceUntilIdle()

        coVerify { repository.importFromText("exit") }
        coVerify { repository.saveWords(imported) }
    }

    @Test
    fun `importFromText with blank string does nothing`() = runTest {
        viewModel.importFromText("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.importFromText(any()) }
    }

    // -------------------------------------------------------------------------
    // Update color
    // -------------------------------------------------------------------------

    @Test
    fun `updateColor delegates to repository`() = runTest {
        viewModel.updateColor("hello", 0xFF0000FF.toInt())
        advanceUntilIdle()

        coVerify { repository.updateColor("hello", 0xFF0000FF.toInt()) }
    }

    // -------------------------------------------------------------------------
    // Profiles flow observation
    // -------------------------------------------------------------------------

    @Test
    fun `profiles from flow appear in ui state`() = runTest {
        val profiles = listOf(WordProfile(1L, "Default"))
        profilesFlow.value = profiles
        advanceUntilIdle()

        assertEquals(profiles, viewModel.uiState.value.profiles)
    }

    @Test
    fun `activeProfileId in ui state reflects repository value when profiles update`() = runTest {
        every { repository.activeProfileId } returns 42L
        profilesFlow.value = listOf(WordProfile(42L, "Work"))
        advanceUntilIdle()

        assertEquals(42L, viewModel.uiState.value.activeProfileId)
    }

    @Test
    fun `empty profiles flow leaves profiles list empty`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.profiles.isEmpty())
    }

    // -------------------------------------------------------------------------
    // switchProfile
    // -------------------------------------------------------------------------

    @Test
    fun `switchProfile updates activeProfileId in ui state immediately`() = runTest {
        viewModel.switchProfile(99L)

        assertEquals(99L, viewModel.uiState.value.activeProfileId)
    }

    @Test
    fun `switchProfile delegates to repository`() = runTest {
        viewModel.switchProfile(99L)

        verify { repository.setActiveProfile(99L) }
    }
}
