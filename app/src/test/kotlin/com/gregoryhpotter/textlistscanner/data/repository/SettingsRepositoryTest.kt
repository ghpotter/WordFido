package com.gregoryhpotter.textlistscanner.data.repository

import android.content.SharedPreferences
import com.gregoryhpotter.textlistscanner.feedback.AudioFeedbackTone
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        editor = mockk(relaxed = true) {
            every { putBoolean(any(), any()) } returns this@mockk
            every { putString(any(), any()) } returns this@mockk
        }
        prefs = mockk {
            every { edit() } returns editor
            every { getBoolean(any(), any()) } answers { secondArg() }
        }
        repository = SettingsRepository(prefs)
    }

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    @Test
    fun `caseSensitive defaults to false`() {
        assertFalse(repository.caseSensitive)
    }

    @Test
    fun `wholeWord defaults to true`() {
        assertTrue(repository.wholeWord)
    }

    @Test
    fun `hapticEnabled defaults to true`() {
        assertTrue(repository.hapticEnabled)
    }

    @Test
    fun `audioEnabled defaults to true`() {
        assertTrue(repository.audioEnabled)
    }

    @Test
    fun `visualEnabled defaults to true`() {
        assertTrue(repository.visualEnabled)
    }

    @Test
    fun `audioTone defaults to Beep`() {
        every { prefs.getString(SettingsRepository.KEY_AUDIO_TONE, any()) } returns AudioFeedbackTone.Beep.name
        assertEquals(AudioFeedbackTone.Beep, repository.audioTone)
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    @Test
    fun `caseSensitive reads correct key`() {
        repository.caseSensitive
        verify { prefs.getBoolean(SettingsRepository.KEY_CASE_SENSITIVE, any()) }
    }

    @Test
    fun `wholeWord reads correct key`() {
        repository.wholeWord
        verify { prefs.getBoolean(SettingsRepository.KEY_WHOLE_WORD, any()) }
    }

    @Test
    fun `hapticEnabled reads correct key`() {
        repository.hapticEnabled
        verify { prefs.getBoolean(SettingsRepository.KEY_HAPTIC, any()) }
    }

    @Test
    fun `audioEnabled reads correct key`() {
        repository.audioEnabled
        verify { prefs.getBoolean(SettingsRepository.KEY_AUDIO, any()) }
    }

    @Test
    fun `visualEnabled reads correct key`() {
        repository.visualEnabled
        verify { prefs.getBoolean(SettingsRepository.KEY_VISUAL, any()) }
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    @Test
    fun `setting caseSensitive writes correct key`() {
        repository.caseSensitive = true
        verify { editor.putBoolean(SettingsRepository.KEY_CASE_SENSITIVE, true) }
        verify { editor.apply() }
    }

    @Test
    fun `setting wholeWord writes correct key`() {
        repository.wholeWord = false
        verify { editor.putBoolean(SettingsRepository.KEY_WHOLE_WORD, false) }
        verify { editor.apply() }
    }

    @Test
    fun `setting hapticEnabled writes correct key`() {
        repository.hapticEnabled = false
        verify { editor.putBoolean(SettingsRepository.KEY_HAPTIC, false) }
        verify { editor.apply() }
    }

    @Test
    fun `setting audioEnabled writes correct key`() {
        repository.audioEnabled = false
        verify { editor.putBoolean(SettingsRepository.KEY_AUDIO, false) }
        verify { editor.apply() }
    }

    @Test
    fun `setting visualEnabled writes correct key`() {
        repository.visualEnabled = false
        verify { editor.putBoolean(SettingsRepository.KEY_VISUAL, false) }
        verify { editor.apply() }
    }

    @Test
    fun `setting audioTone writes correct key`() {
        repository.audioTone = AudioFeedbackTone.Beep
        verify { editor.putString(SettingsRepository.KEY_AUDIO_TONE, "Beep") }
        verify { editor.apply() }
    }
}