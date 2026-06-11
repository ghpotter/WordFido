package com.gregoryhpotter.textlistscanner.data.repository

import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists the word list as a JSON flat file.
 *
 * All operations are main-safe — file I/O is dispatched to [Dispatchers.IO].
 */
class WordListRepository(private val storageFile: File) {

    private val _wordsFlow = MutableStateFlow<List<WordEntry>>(emptyList())
    val wordsFlow: Flow<List<WordEntry>> = _wordsFlow.asStateFlow()

    init {
        // Initial load
        val initialWords = try {
            if (storageFile.exists() && storageFile.length() > 0L) {
                parseJson(storageFile.readText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        _wordsFlow.value = initialWords
    }

    // -------------------------------------------------------------------------
    // Default colors assigned to imported words (cycles through the list)
    // -------------------------------------------------------------------------

    private val defaultColors = listOf(
        0xFFE53935.toInt(), // red
        0xFF1E88E5.toInt(), // blue
        0xFF43A047.toInt(), // green
        0xFFFB8C00.toInt(), // orange
        0xFF8E24AA.toInt(), // purple
        0xFF00ACC1.toInt(), // cyan
        0xFFFFB300.toInt(), // amber
        0xFF6D4C41.toInt()  // brown
    )

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    suspend fun loadWords(): List<WordEntry> = withContext(Dispatchers.IO) {
        val words = if (!storageFile.exists() || storageFile.length() == 0L) emptyList()
        else {
            try {
                parseJson(storageFile.readText())
            } catch (e: Exception) {
                emptyList()
            }
        }
        _wordsFlow.value = words
        words
    }

    suspend fun saveWords(words: List<WordEntry>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        words.forEach { entry ->
            val obj = JSONObject().apply {
                put(KEY_TEXT, entry.text)
                put(KEY_COLOR, entry.color)
                put(KEY_ENABLED, entry.enabled)
            }
            array.put(obj)
        }
        storageFile.writeText(array.toString())
        _wordsFlow.value = words
    }

    suspend fun addWord(entry: WordEntry) {
        val current = _wordsFlow.value.toMutableList()
        val alreadyExists = current.any {
            it.text.equals(entry.text, ignoreCase = true)
        }
        if (!alreadyExists) {
            current.add(entry)
            saveWords(current)
        }
    }

    suspend fun removeWord(text: String) {
        val current = _wordsFlow.value.toMutableList()
        current.removeAll { it.text.equals(text, ignoreCase = true) }
        saveWords(current)
    }

    suspend fun setWordEnabled(text: String, enabled: Boolean) {
        val current = _wordsFlow.value.map { entry ->
            if (entry.text.equals(text, ignoreCase = true)) entry.copy(enabled = enabled)
            else entry
        }
        saveWords(current)
    }

    suspend fun updateColor(text: String, color: Int) {
        val current = _wordsFlow.value.map { entry ->
            if (entry.text.equals(text, ignoreCase = true)) entry.copy(color = color)
            else entry
        }
        saveWords(current)
    }

    /**
     * Parses a plain-text string of words separated by commas or newlines.
     * Deduplicates case-insensitively and assigns default colors.
     * Does NOT automatically persist — caller decides whether to save.
     */
    fun importFromText(raw: String): List<WordEntry> {
        val seen = mutableSetOf<String>()
        return raw
            .split(Regex("[,\n]"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { word -> seen.add(word.lowercase()) }
            .mapIndexed { index, word ->
                WordEntry(
                    text = word,
                    color = defaultColors[index % defaultColors.size],
                    enabled = true
                )
            }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun parseJson(json: String): List<WordEntry> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            WordEntry(
                text = obj.getString(KEY_TEXT),
                color = obj.getInt(KEY_COLOR),
                enabled = obj.getBoolean(KEY_ENABLED)
            )
        }
    }

    companion object {
        private const val KEY_TEXT = "text"
        private const val KEY_COLOR = "color"
        private const val KEY_ENABLED = "enabled"
    }
}