package com.gregoryhpotter.textlistscanner.data.repository

import androidx.room.withTransaction
import com.gregoryhpotter.textlistscanner.data.db.AppDatabase
import com.gregoryhpotter.textlistscanner.data.db.WordProfileEntity
import com.gregoryhpotter.textlistscanner.data.db.toEntity
import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.data.model.WordProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordListRepository @Inject constructor(
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val _activeProfileId = MutableStateFlow(settingsRepository.activeProfileId)

    val activeProfileId: Long get() = _activeProfileId.value

    val profilesFlow: Flow<List<WordProfile>> = db.wordProfileDao().watchAll()
        .map { entities -> entities.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val wordsFlow: Flow<List<WordEntry>> = _activeProfileId.flatMapLatest { profileId ->
        if (profileId < 0L) flowOf(emptyList())
        else db.wordEntryDao().watchByProfile(profileId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    // -------------------------------------------------------------------------
    // Profile management
    // -------------------------------------------------------------------------

    suspend fun ensureDefaultProfile() {
        var profileId = settingsRepository.activeProfileId
        if (profileId >= 0L) {
            _activeProfileId.value = profileId
            return
        }
        val existing = db.wordProfileDao().getAll()
        profileId = if (existing.isNotEmpty()) {
            existing.first().id
        } else {
            db.wordProfileDao().insert(WordProfileEntity(name = "Default"))
        }
        setActiveProfile(profileId)
    }

    fun setActiveProfile(id: Long) {
        settingsRepository.activeProfileId = id
        _activeProfileId.value = id
    }

    suspend fun createProfile(name: String): Long =
        db.wordProfileDao().insert(WordProfileEntity(name = name.trim()))

    suspend fun deleteProfile(id: Long) {
        db.wordProfileDao().deleteById(id)
        if (_activeProfileId.value == id) {
            val remaining = db.wordProfileDao().getAll()
            val nextId = remaining.firstOrNull()?.id
                ?: db.wordProfileDao().insert(WordProfileEntity(name = "Default"))
            setActiveProfile(nextId)
        }
    }

    suspend fun renameProfile(id: Long, name: String) =
        db.wordProfileDao().rename(id, name.trim())

    // -------------------------------------------------------------------------
    // Word management (scoped to active profile)
    // -------------------------------------------------------------------------

    suspend fun loadWords(): List<WordEntry> {
        val profileId = _activeProfileId.value
        if (profileId < 0L) return emptyList()
        return db.wordEntryDao().getByProfile(profileId).map { it.toDomain() }
    }

    suspend fun addWord(entry: WordEntry) {
        val profileId = _activeProfileId.value
        if (profileId < 0L) return
        if (db.wordEntryDao().countByText(profileId, entry.text) == 0) {
            db.wordEntryDao().insert(entry.toEntity(profileId))
        }
    }

    suspend fun removeWord(text: String) {
        val profileId = _activeProfileId.value
        if (profileId < 0L) return
        db.wordEntryDao().deleteByText(profileId, text)
    }

    suspend fun setWordEnabled(text: String, enabled: Boolean) {
        val profileId = _activeProfileId.value
        if (profileId < 0L) return
        db.wordEntryDao().setEnabled(profileId, text, enabled)
    }

    suspend fun updateColor(text: String, color: Int) {
        val profileId = _activeProfileId.value
        if (profileId < 0L) return
        db.wordEntryDao().updateColor(profileId, text, color)
    }

    suspend fun saveWords(words: List<WordEntry>) {
        val profileId = _activeProfileId.value
        if (profileId < 0L) return
        db.withTransaction {
            db.wordEntryDao().deleteAllByProfile(profileId)
            db.wordEntryDao().insertAll(words.map { it.toEntity(profileId) })
        }
    }

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

    private val defaultColors = listOf(
        0xFFE53935.toInt(),
        0xFF1E88E5.toInt(),
        0xFF43A047.toInt(),
        0xFFFB8C00.toInt(),
        0xFF8E24AA.toInt(),
        0xFF00ACC1.toInt(),
        0xFFFFB300.toInt(),
        0xFF6D4C41.toInt()
    )
}
