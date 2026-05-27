package com.gregoryhpotter.textlistscanner.ui.wordlist

import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.data.repository.WordListRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class WordListViewModelTest {

    private lateinit var repository: WordListRepository
    private lateinit var viewModel: WordListViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.loadWords() } returns emptyList()
        viewModel = WordListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Initial load
    // -------------------------------------------------------------------------

    @Test
    fun `word list loads on init`() = runTest {
        val words = listOf(WordEntry("exit", 0xFF0000, true))
        coEvery { repository.loadWords() } returns words

        viewModel = WordListViewModel(repository)
        advanceUntilIdle()

        assertEquals(words, viewModel.uiState.value.words)
    }

    @Test
    fun `ui state starts with empty list`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.words.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Add word
    // -------------------------------------------------------------------------

    @Test
    fun `addWord calls repository and reloads list`() = runTest {
        viewModel.addWord("exit", 0xFF0000)
        advanceUntilIdle()

        coVerify { repository.addWord(match { it.text == "exit" && it.color == 0xFF0000 }) }
        coVerify(atLeast = 2) { repository.loadWords() }
    }

    @Test
    fun `addWord trims whitespace from text`() = runTest {
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
    // Remove word
    // -------------------------------------------------------------------------

    @Test
    fun `removeWord calls repository with correct text`() = runTest {
        viewModel.removeWord("exit")
        advanceUntilIdle()

        coVerify { repository.removeWord("exit") }
    }

    @Test
    fun `removeWord reloads list after deletion`() = runTest {
        viewModel.removeWord("exit")
        advanceUntilIdle()

        coVerify(atLeast = 2) { repository.loadWords() }
    }

    // -------------------------------------------------------------------------
    // Toggle enabled
    // -------------------------------------------------------------------------

    @Test
    fun `toggleWord enables a disabled word`() = runTest {
        val words = listOf(WordEntry("exit", 0xFF0000, enabled = false))
        coEvery { repository.loadWords() } returns words
        viewModel = WordListViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleWord("exit")
        advanceUntilIdle()

        coVerify { repository.setWordEnabled("exit", true) }
    }

    @Test
    fun `toggleWord disables an enabled word`() = runTest {
        val words = listOf(WordEntry("exit", 0xFF0000, enabled = true))
        coEvery { repository.loadWords() } returns words
        viewModel = WordListViewModel(repository)
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
        val imported = listOf(
            WordEntry("exit", 0xFF0000, true),
            WordEntry("door", 0x00FF00, true)
        )
        coEvery { repository.importFromText(any()) } returns imported

        viewModel.importFromText("exit,door")
        advanceUntilIdle()

        coVerify { repository.importFromText("exit,door") }
        coVerify { repository.saveWords(imported) }
    }

    @Test
    fun `importFromText with blank string does nothing`() = runTest {
        viewModel.importFromText("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.importFromText(any()) }
    }

    @Test
    fun `importFromText reloads list after import`() = runTest {
        coEvery { repository.importFromText(any()) } returns listOf(
            WordEntry("exit", 0xFF0000, true)
        )

        viewModel.importFromText("exit")
        advanceUntilIdle()

        coVerify(atLeast = 2) { repository.loadWords() }
    }

    // -------------------------------------------------------------------------
    // UI state — error and loading
    // -------------------------------------------------------------------------

    @Test
    fun `ui state reflects loading while words are being fetched`() = runTest {
        coEvery { repository.loadWords() } coAnswers {
            kotlinx.coroutines.delay(1000)
            emptyList()
        }
        val testViewModel = WordListViewModel(repository)
        testDispatcher.scheduler.runCurrent()
        assertTrue(testViewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(testViewModel.uiState.value.isLoading)
    }

    @Test
    fun `ui state error is null on successful load`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.error == null)
    }

    @Test
    fun `updateColor delegates to repository`() = runTest {
        viewModel.updateColor("hello", 0xFF0000FF.toInt())
        advanceUntilIdle()

        coVerify { repository.updateColor("hello", 0xFF0000FF.toInt()) }
    }
}