package com.gregoryhpotter.textlistscanner.feedback

import javax.inject.Inject

/**
 * Orchestrates haptic and audio feedback when watched words are detected.
 *
 * Deduplication: feedback fires only the first time a word enters the frame.
 * Call [onWordsCleared] when the word list changes or the camera frame no
 * longer contains previously seen words, so feedback fires again on re-entry.
 *
 * Both feedback types are independently togglable at runtime.
 */
class FeedbackManager @Inject constructor(
    private val hapticProvider: HapticFeedbackProvider,
    private val audioProvider: AudioFeedbackProvider
) {

    private var hapticEnabled: Boolean = true
    private var audioEnabled: Boolean = true

    // Words seen in the current detection cycle — prevents repeated feedback
    // on every OCR frame while a word remains in view
    private val seenWords = mutableSetOf<String>()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Call when a matched word is detected in a camera frame.
     * Feedback fires only if this word has not been seen since the last
     * [onWordsCleared] call.
     */
    fun onWordDetected(word: String): Boolean {
        return if (seenWords.add(word)) {
            if (hapticEnabled) hapticProvider.vibrate()
            if (audioEnabled) audioProvider.playTone()
            true
        } else {
            false
        }
    }

    /**
     * Call when the set of visible words changes — e.g. when the camera
     * moves away and the word is no longer in frame, or when the word
     * list is updated. Resets deduplication so feedback fires again
     * on next detection.
     */
    fun onWordsCleared() {
        seenWords.clear()
    }

    fun setHapticEnabled(enabled: Boolean) {
        hapticEnabled = enabled
    }

    fun setAudioEnabled(enabled: Boolean) {
        audioEnabled = enabled
    }

    fun setAudioTone(tone: AudioFeedbackTone) {
        audioProvider.setTone(tone)
    }

    fun release() {
        audioProvider.release()
    }
}