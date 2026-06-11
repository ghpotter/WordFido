package com.gregoryhpotter.textlistscanner.data.repository

import android.content.SharedPreferences
import com.gregoryhpotter.textlistscanner.feedback.AudioFeedbackTone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val prefs: SharedPreferences
) {
    var caseSensitive: Boolean
        get() = prefs.getBoolean(KEY_CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE)
        set(value) { prefs.edit().putBoolean(KEY_CASE_SENSITIVE, value).apply() }

    var wholeWord: Boolean
        get() = prefs.getBoolean(KEY_WHOLE_WORD, DEFAULT_WHOLE_WORD)
        set(value) { prefs.edit().putBoolean(KEY_WHOLE_WORD, value).apply() }

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, DEFAULT_HAPTIC)
        set(value) { prefs.edit().putBoolean(KEY_HAPTIC, value).apply() }

    var audioEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO, DEFAULT_AUDIO)
        set(value) { prefs.edit().putBoolean(KEY_AUDIO, value).apply() }

    var audioTone: AudioFeedbackTone
        get() {
            val name = prefs.getString(KEY_AUDIO_TONE, DEFAULT_AUDIO_TONE.name)
            return AudioFeedbackTone.entries.find { it.name == name } ?: DEFAULT_AUDIO_TONE
        }
        set(value) { prefs.edit().putString(KEY_AUDIO_TONE, value.name).apply() }

    companion object {
        const val KEY_CASE_SENSITIVE = "case_sensitive"
        const val KEY_WHOLE_WORD = "whole_word"
        const val KEY_HAPTIC = "haptic_enabled"
        const val KEY_AUDIO = "audio_enabled"

        const val DEFAULT_CASE_SENSITIVE = false
        const val DEFAULT_WHOLE_WORD = true
        const val DEFAULT_HAPTIC = true
        const val DEFAULT_AUDIO = true
        const val KEY_AUDIO_TONE = "audio_tone"
        val DEFAULT_AUDIO_TONE = AudioFeedbackTone.Beep
    }
}