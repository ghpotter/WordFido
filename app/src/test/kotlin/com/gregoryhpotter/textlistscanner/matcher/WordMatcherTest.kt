package com.gregoryhpotter.textlistscanner.matcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Tests for WordMatcher.
 *
 * Matching rules:
 *  - Punctuation is always stripped before comparison
 *  - Case sensitivity is user-togglable (default: OFF = case-insensitive)
 *  - Whole-word matching is user-togglable (default: ON = whole-word only)
 */
class WordMatcherTest {

    private lateinit var matcher: WordMatcher

    @Before
    fun setUp() {
        matcher = WordMatcher()
    }

    // -------------------------------------------------------------------------
    // Punctuation stripping
    // -------------------------------------------------------------------------

    @Test
    fun `word with trailing period matches target`() {
        val result = matcher.findMatches(
            text = "exit.",
            wordList = listOf("exit")
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `word with trailing comma matches target`() {
        val result = matcher.findMatches(
            text = "exit,",
            wordList = listOf("exit")
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `word with trailing exclamation matches target`() {
        val result = matcher.findMatches(
            text = "exit!",
            wordList = listOf("exit")
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `word with leading and trailing punctuation matches target`() {
        val result = matcher.findMatches(
            text = "\"exit\"",
            wordList = listOf("exit")
        )
        assertTrue(result.contains("exit"))
    }

    // -------------------------------------------------------------------------
    // Case sensitivity — OFF (default, case-insensitive)
    // -------------------------------------------------------------------------

    @Test
    fun `uppercase input matches lowercase target when case insensitive`() {
        val result = matcher.findMatches(
            text = "EXIT",
            wordList = listOf("exit"),
            caseSensitive = false
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `mixed case input matches lowercase target when case insensitive`() {
        val result = matcher.findMatches(
            text = "Exit",
            wordList = listOf("exit"),
            caseSensitive = false
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `match is case insensitive by default`() {
        val result = matcher.findMatches(
            text = "Exit",
            wordList = listOf("exit")
        )
        assertTrue(result.contains("exit"))
    }

    // -------------------------------------------------------------------------
    // Case sensitivity — ON (case-sensitive)
    // -------------------------------------------------------------------------

    @Test
    fun `uppercase input does not match lowercase target when case sensitive`() {
        val result = matcher.findMatches(
            text = "EXIT",
            wordList = listOf("exit"),
            caseSensitive = true
        )
        assertFalse(result.contains("exit"))
    }

    @Test
    fun `exact case input matches when case sensitive`() {
        val result = matcher.findMatches(
            text = "Exit",
            wordList = listOf("Exit"),
            caseSensitive = true
        )
        assertTrue(result.contains("Exit"))
    }

    // -------------------------------------------------------------------------
    // Whole-word matching — ON (default)
    // -------------------------------------------------------------------------

    @Test
    fun `partial match does not match when whole word is on`() {
        val result = matcher.findMatches(
            text = "exiting",
            wordList = listOf("exit"),
            wholeWord = true
        )
        assertFalse(result.contains("exit"))
    }

    @Test
    fun `exact word matches when whole word is on`() {
        val result = matcher.findMatches(
            text = "exit",
            wordList = listOf("exit"),
            wholeWord = true
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `whole word matching is off by default`() {
        val result = matcher.findMatches(
            text = "exiting",
            wordList = listOf("exit")
        )
        assertTrue(result.contains("exit"))
    }

    // -------------------------------------------------------------------------
    // Whole-word matching — OFF (partial allowed)
    // -------------------------------------------------------------------------

    @Test
    fun `partial match succeeds when whole word is off`() {
        val result = matcher.findMatches(
            text = "exiting",
            wordList = listOf("exit"),
            wholeWord = false
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `substring at start of word matches when whole word is off`() {
        val result = matcher.findMatches(
            text = "preview",
            wordList = listOf("pre"),
            wholeWord = false
        )
        assertTrue(result.contains("pre"))
    }

    @Test
    fun `substring at end of word matches when whole word is off`() {
        val result = matcher.findMatches(
            text = "preview",
            wordList = listOf("view"),
            wholeWord = false
        )
        assertTrue(result.contains("view"))
    }

    // -------------------------------------------------------------------------
    // Multiple words in text
    // -------------------------------------------------------------------------

    @Test
    fun `finds multiple matches in a sentence`() {
        val result = matcher.findMatches(
            text = "Please use the exit near the door",
            wordList = listOf("exit", "door")
        )
        assertTrue(result.contains("exit"))
        assertTrue(result.contains("door"))
    }

    @Test
    fun `returns only words that are present in text`() {
        val result = matcher.findMatches(
            text = "Please use the exit",
            wordList = listOf("exit", "door")
        )
        assertTrue(result.contains("exit"))
        assertFalse(result.contains("door"))
    }

    @Test
    fun `finds match anywhere in multi-word text`() {
        val result = matcher.findMatches(
            text = "The emergency exit is on the left",
            wordList = listOf("exit")
        )
        assertTrue(result.contains("exit"))
    }

    // -------------------------------------------------------------------------
    // Combined rules
    // -------------------------------------------------------------------------

    @Test
    fun `punctuation stripped before whole word check`() {
        val result = matcher.findMatches(
            text = "Use the exit. Now.",
            wordList = listOf("exit"),
            wholeWord = true
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `punctuation stripped before case sensitive check`() {
        val result = matcher.findMatches(
            text = "EXIT!",
            wordList = listOf("exit"),
            caseSensitive = false,
            wholeWord = true
        )
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `case sensitive and whole word both enforced together`() {
        val result = matcher.findMatches(
            text = "Exiting now",
            wordList = listOf("exit"),
            caseSensitive = true,
            wholeWord = true
        )
        assertFalse(result.contains("exit"))
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `empty word list returns empty result`() {
        val result = matcher.findMatches(
            text = "exit",
            wordList = emptyList()
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty input text returns empty result`() {
        val result = matcher.findMatches(
            text = "",
            wordList = listOf("exit")
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `blank input text returns empty result`() {
        val result = matcher.findMatches(
            text = "   ",
            wordList = listOf("exit")
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `no matches returns empty result`() {
        val result = matcher.findMatches(
            text = "The door is open",
            wordList = listOf("exit", "emergency")
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `word list with blank entries ignores blanks`() {
        val result = matcher.findMatches(
            text = "exit",
            wordList = listOf("exit", "", "  ")
        )
        assertEquals(1, result.size)
        assertTrue(result.contains("exit"))
    }

    @Test
    fun `duplicate words in word list do not produce duplicate results`() {
        val result = matcher.findMatches(
            text = "exit",
            wordList = listOf("exit", "exit")
        )
        assertEquals(1, result.size)
    }
}