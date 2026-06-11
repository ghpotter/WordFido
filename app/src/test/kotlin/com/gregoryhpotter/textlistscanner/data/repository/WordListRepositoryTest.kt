package com.gregoryhpotter.textlistscanner.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gregoryhpotter.textlistscanner.data.db.AppDatabase
import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordListRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WordListRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val prefs = context.getSharedPreferences("test_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        repository = WordListRepository(db, SettingsRepository(prefs))
        runBlocking { repository.ensureDefaultProfile() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `fresh repository has no words`() = runBlocking {
        assertTrue(repository.loadWords().isEmpty())
    }

    // -------------------------------------------------------------------------
    // Add and load
    // -------------------------------------------------------------------------

    @Test
    fun `added word can be loaded back`() = runBlocking {
        repository.addWord(WordEntry(text = "exit", color = 0xFF0000, enabled = true))
        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
        assertEquals("exit", loaded.first().text)
    }

    @Test
    fun `saved color is preserved`() = runBlocking {
        repository.addWord(WordEntry(text = "exit", color = 0xFF0000, enabled = true))
        assertEquals(0xFF0000, repository.loadWords().first().color)
    }

    @Test
    fun `saved enabled state is preserved`() = runBlocking {
        repository.addWord(WordEntry(text = "exit", color = 0xFF0000, enabled = false))
        assertFalse(repository.loadWords().first().enabled)
    }

    @Test
    fun `multiple words are all saved`() = runBlocking {
        repository.addWord(WordEntry("exit", 0xFF0000, true))
        repository.addWord(WordEntry("door", 0x00FF00, true))
        repository.addWord(WordEntry("stairs", 0x0000FF, false))
        assertEquals(3, repository.loadWords().size)
    }

    // -------------------------------------------------------------------------
    // saveWords replaces the list
    // -------------------------------------------------------------------------

    @Test
    fun `saveWords replaces existing words`() = runBlocking {
        repository.addWord(WordEntry("exit", 0xFF0000, true))
        repository.saveWords(listOf(WordEntry("door", 0x00FF00, true)))
        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
        assertEquals("door", loaded.first().text)
    }

    // -------------------------------------------------------------------------
    // Remove
    // -------------------------------------------------------------------------

    @Test
    fun `removeWord deletes the entry`() = runBlocking {
        repository.addWord(WordEntry("exit", 0xFF0000, true))
        repository.addWord(WordEntry("door", 0x00FF00, true))
        repository.removeWord("exit")
        val loaded = repository.loadWords()
        assertEquals(1, loaded.size)
        assertEquals("door", loaded.first().text)
    }

    @Test
    fun `removeWord on non-existent word is a no-op`() = runBlocking {
        repository.addWord(WordEntry("exit", 0xFF0000, true))
        repository.removeWord("door")
        assertEquals(1, repository.loadWords().size)
    }

    @Test
    fun `addWord does not add duplicate text`() = runBlocking {
        repository.addWord(WordEntry("exit", 0xFF0000, true))
        repository.addWord(WordEntry("exit", 0x00FF00, true))
        assertEquals(1, repository.loadWords().size)
    }

    // -------------------------------------------------------------------------
    // Enable / disable
    // -------------------------------------------------------------------------

    @Test
    fun `setWordEnabled updates enabled state`() = runBlocking {
        repository.addWord(WordEntry("exit", 0xFF0000, true))
        repository.setWordEnabled("exit", false)
        assertFalse(repository.loadWords().first().enabled)
    }

    // -------------------------------------------------------------------------
    // Color
    // -------------------------------------------------------------------------

    @Test
    fun `updateColor changes color for matching word`() = runBlocking {
        repository.addWord(WordEntry("hello", 0xFFFF0000.toInt(), true))
        repository.updateColor("hello", 0xFF0000FF.toInt())
        assertEquals(0xFF0000FF.toInt(), repository.loadWords().first().color)
    }

    @Test
    fun `updateColor does not affect other words`() = runBlocking {
        repository.addWord(WordEntry("hello", 0xFFFF0000.toInt(), true))
        repository.addWord(WordEntry("world", 0xFF00FF00.toInt(), true))
        repository.updateColor("hello", 0xFF0000FF.toInt())
        assertEquals(0xFF00FF00.toInt(), repository.loadWords().first { it.text == "world" }.color)
    }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    @Test
    fun `importFromText parses comma separated words`() {
        val imported = repository.importFromText("exit,door,stairs")
        assertEquals(listOf("exit", "door", "stairs"), imported.map { it.text })
    }

    @Test
    fun `importFromText parses newline separated words`() {
        val imported = repository.importFromText("exit\ndoor\nstairs")
        assertEquals(3, imported.size)
    }

    @Test
    fun `importFromText trims whitespace`() {
        val imported = repository.importFromText("  exit , door , stairs  ")
        assertEquals(listOf("exit", "door", "stairs"), imported.map { it.text })
    }

    @Test
    fun `importFromText ignores blank entries`() {
        val imported = repository.importFromText("exit,,door,  ,stairs")
        assertEquals(3, imported.size)
    }

    @Test
    fun `importFromText deduplicates case-insensitively`() {
        val imported = repository.importFromText("exit,Exit,EXIT")
        assertEquals(1, imported.size)
    }

    // -------------------------------------------------------------------------
    // Multiple profiles
    // -------------------------------------------------------------------------

    @Test
    fun `words from different profiles are isolated`() = runBlocking {
        repository.addWord(WordEntry("grocery", 0xFF0000, true))

        val newProfileId = repository.createProfile("Work")
        repository.setActiveProfile(newProfileId)
        repository.addWord(WordEntry("invoice", 0x00FF00, true))

        assertEquals(listOf("invoice"), repository.loadWords().map { it.text })

        repository.setActiveProfile(repository.activeProfileId.let {
            // switch back to original profile
            val profiles = db.wordProfileDao().getAll()
            profiles.first { p -> p.id != newProfileId }.id
        })
        assertEquals(listOf("grocery"), repository.loadWords().map { it.text })
    }

    @Test
    fun `deleting a profile deletes its words`() = runBlocking {
        val secondId = repository.createProfile("Temp")
        repository.setActiveProfile(secondId)
        repository.addWord(WordEntry("temp", 0xFF0000, true))

        repository.deleteProfile(secondId)

        // active profile switched away; no "temp" word in new active profile
        assertFalse(repository.loadWords().any { it.text == "temp" })
    }
}
