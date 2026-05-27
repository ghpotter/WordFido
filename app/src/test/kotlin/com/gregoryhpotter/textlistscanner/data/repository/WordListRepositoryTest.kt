package com.gregoryhpotter.textlistscanner.data.repository

import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Tests for WordListRepository.
 *
 * Uses JUnit's TemporaryFolder rule to create a real but disposable
 * file system — no mocking needed for file I/O.
 */
@RunWith(RobolectricTestRunner::class)
class WordListRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: WordListRepository
    private lateinit var storageFile: File

    @Before
    fun setUp() {
        storageFile = tempFolder.newFile("word_list.json")
        repository = WordListRepository(storageFile)
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `empty file returns empty list`() = runTest {
        val words = repository.loadWords()
        assertTrue(words.isEmpty())
    }

    @Test
    fun `fresh repository has no words`() = runTest {
        assertEquals(0, repository.loadWords().size)
    }

    // -------------------------------------------------------------------------
    // Save and load
    // -------------------------------------------------------------------------

    @Test
    fun `saved word can be loaded back`() = runTest {
        val entry = WordEntry(text = "exit", color = 0xFF0000, enabled = true)
        repository.saveWords(listOf(entry))

        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
        assertEquals("exit", loaded.first().text)
    }

    @Test
    fun `saved color is preserved after load`() = runTest {
        val entry = WordEntry(text = "exit", color = 0xFF0000, enabled = true)
        repository.saveWords(listOf(entry))

        val loaded = repository.loadWords()
        assertEquals(0xFF0000, loaded.first().color)
    }

    @Test
    fun `saved enabled state is preserved after load`() = runTest {
        val entry = WordEntry(text = "exit", color = 0xFF0000, enabled = false)
        repository.saveWords(listOf(entry))

        val loaded = repository.loadWords()
        assertFalse(loaded.first().enabled)
    }

    @Test
    fun `multiple words are all saved and loaded`() = runTest {
        val entries = listOf(
            WordEntry(text = "exit", color = 0xFF0000, enabled = true),
            WordEntry(text = "door", color = 0x00FF00, enabled = true),
            WordEntry(text = "stairs", color = 0x0000FF, enabled = false)
        )
        repository.saveWords(entries)

        val loaded = repository.loadWords()
        assertEquals(3, loaded.size)
        assertEquals(listOf("exit", "door", "stairs"), loaded.map { it.text })
    }

    @Test
    fun `saving overwrites previous list`() = runTest {
        repository.saveWords(listOf(WordEntry(text = "exit", color = 0xFF0000, enabled = true)))
        repository.saveWords(listOf(WordEntry(text = "door", color = 0x00FF00, enabled = true)))

        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
        assertEquals("door", loaded.first().text)
    }

    // -------------------------------------------------------------------------
    // Add and remove individual entries
    // -------------------------------------------------------------------------

    @Test
    fun `addWord appends to existing list`() = runTest {
        repository.saveWords(listOf(WordEntry(text = "exit", color = 0xFF0000, enabled = true)))
        repository.addWord(WordEntry(text = "door", color = 0x00FF00, enabled = true))

        val loaded = repository.loadWords()
        assertEquals(2, loaded.size)
    }

    @Test
    fun `removeWord deletes entry by text`() = runTest {
        repository.saveWords(listOf(
            WordEntry(text = "exit", color = 0xFF0000, enabled = true),
            WordEntry(text = "door", color = 0x00FF00, enabled = true)
        ))
        repository.removeWord("exit")

        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
        assertEquals("door", loaded.first().text)
    }

    @Test
    fun `removeWord on non-existent word leaves list unchanged`() = runTest {
        repository.saveWords(listOf(WordEntry(text = "exit", color = 0xFF0000, enabled = true)))
        repository.removeWord("door")

        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
    }

    @Test
    fun `addWord does not add duplicate text`() = runTest {
        repository.saveWords(listOf(WordEntry(text = "exit", color = 0xFF0000, enabled = true)))
        repository.addWord(WordEntry(text = "exit", color = 0x00FF00, enabled = true))

        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
    }

    // -------------------------------------------------------------------------
    // Enable / disable
    // -------------------------------------------------------------------------

    @Test
    fun `setWordEnabled updates enabled state`() = runTest {
        repository.saveWords(listOf(WordEntry(text = "exit", color = 0xFF0000, enabled = true)))
        repository.setWordEnabled("exit", false)

        val loaded = repository.loadWords()
        assertFalse(loaded.first().enabled)
    }

    @Test
    fun `setWordEnabled on non-existent word does not throw`() = runTest {
        repository.saveWords(listOf(WordEntry(text = "exit", color = 0xFF0000, enabled = true)))
        repository.setWordEnabled("door", false) // should be a no-op

        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
    }

    // -------------------------------------------------------------------------
    // Import from CSV / TXT
    // -------------------------------------------------------------------------

    @Test
    fun `importFromText parses comma separated words`() = runTest {
        val imported = repository.importFromText("exit,door,stairs")
        assertEquals(3, imported.size)
        assertEquals(listOf("exit", "door", "stairs"), imported.map { it.text })
    }

    @Test
    fun `importFromText parses newline separated words`() = runTest {
        val imported = repository.importFromText("exit\ndoor\nstairs")
        assertEquals(3, imported.size)
    }

    @Test
    fun `importFromText trims whitespace from each word`() = runTest {
        val imported = repository.importFromText("  exit , door , stairs  ")
        assertEquals(listOf("exit", "door", "stairs"), imported.map { it.text })
    }

    @Test
    fun `importFromText ignores blank entries`() = runTest {
        val imported = repository.importFromText("exit,,door,  ,stairs")
        assertEquals(3, imported.size)
    }

    @Test
    fun `importFromText deduplicates words case-insensitively`() = runTest {
        val imported = repository.importFromText("exit,Exit,EXIT")
        assertEquals(1, imported.size)
    }

    @Test
    fun `importFromText assigns default color and enabled state`() = runTest {
        val imported = repository.importFromText("exit")
        assertTrue(imported.first().enabled)
        // Color should be a valid non-zero ARGB value
        assertTrue(imported.first().color != 0)
    }

    // -------------------------------------------------------------------------
    // Color tests
    // -------------------------------------------------------------------------

    @Test
    fun `updateColor changes color for matching word`() = runTest {
        repository.addWord(WordEntry("hello", 0xFFFF0000.toInt(), true))
        repository.updateColor("hello", 0xFF0000FF.toInt())
        assertEquals(0xFF0000FF.toInt(), repository.loadWords().first { it.text == "hello" }.color)
    }

    @Test
    fun `updateColor does not affect other words`() = runTest {
        repository.addWord(WordEntry("hello", 0xFFFF0000.toInt(), true))
        repository.addWord(WordEntry("world", 0xFF00FF00.toInt(), true))
        repository.updateColor("hello", 0xFF0000FF.toInt())
        assertEquals(0xFF00FF00.toInt(), repository.loadWords().first { it.text == "world" }.color)
    }

    @Test
    fun `updateColor is a no-op for unknown word`() = runTest {
        repository.addWord(WordEntry("hello", 0xFFFF0000.toInt(), true))
        repository.updateColor("unknown", 0xFF0000FF.toInt())
        assertEquals(0xFFFF0000.toInt(), repository.loadWords().first().color)
    }
}