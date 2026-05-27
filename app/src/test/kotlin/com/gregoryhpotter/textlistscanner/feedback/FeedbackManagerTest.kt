package com.gregoryhpotter.textlistscanner.feedback

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Tests for [FeedbackManager].
 *
 * Hardware interactions (vibrator, audio) are abstracted behind
 * [HapticFeedbackProvider] and [AudioFeedbackProvider] interfaces
 * so they can be mocked here without Android dependencies.
 */
class FeedbackManagerTest {

    private lateinit var hapticProvider: HapticFeedbackProvider
    private lateinit var audioProvider: AudioFeedbackProvider
    private lateinit var manager: FeedbackManager

    @Before
    fun setUp() {
        hapticProvider = mockk(relaxed = true)
        audioProvider = mockk(relaxed = true)
        manager = FeedbackManager(hapticProvider, audioProvider)
    }

    // -------------------------------------------------------------------------
    // Default state — all feedback enabled
    // -------------------------------------------------------------------------

    @Test
    fun `haptic fires when a new word is detected and haptic is enabled`() {
        manager.onWordDetected("exit")
        verify { hapticProvider.vibrate() }
    }

    @Test
    fun `audio fires when a new word is detected and audio is enabled`() {
        manager.onWordDetected("exit")
        verify { audioProvider.playTone() }
    }

    // -------------------------------------------------------------------------
    // Haptic toggle
    // -------------------------------------------------------------------------

    @Test
    fun `haptic does not fire when haptic is disabled`() {
        manager.setHapticEnabled(false)
        manager.onWordDetected("exit")
        verify { hapticProvider wasNot Called }
    }

    @Test
    fun `haptic fires again after being re-enabled`() {
        manager.setHapticEnabled(false)
        manager.setHapticEnabled(true)
        manager.onWordDetected("exit")
        verify { hapticProvider.vibrate() }
    }

    // -------------------------------------------------------------------------
    // Audio toggle
    // -------------------------------------------------------------------------

    @Test
    fun `audio does not fire when audio is disabled`() {
        manager.setAudioEnabled(false)
        manager.onWordDetected("exit")
        verify { audioProvider wasNot Called }
    }

    @Test
    fun `audio fires again after being re-enabled`() {
        manager.setAudioEnabled(false)
        manager.setAudioEnabled(true)
        manager.onWordDetected("exit")
        verify { audioProvider.playTone() }
    }

    // -------------------------------------------------------------------------
    // Both disabled
    // -------------------------------------------------------------------------

    @Test
    fun `no feedback fires when both are disabled`() {
        manager.setHapticEnabled(false)
        manager.setAudioEnabled(false)
        manager.onWordDetected("exit")
        verify { hapticProvider wasNot Called }
        verify { audioProvider wasNot Called }
    }

    // -------------------------------------------------------------------------
    // Deduplication — only fires once per word per detection cycle
    // -------------------------------------------------------------------------

    @Test
    fun `feedback fires only once for repeated detection of same word`() {
        manager.onWordDetected("exit")
        manager.onWordDetected("exit")
        manager.onWordDetected("exit")
        verify(exactly = 1) { hapticProvider.vibrate() }
        verify(exactly = 1) { audioProvider.playTone() }
    }

    @Test
    fun `feedback fires for each distinct new word`() {
        manager.onWordDetected("exit")
        manager.onWordDetected("door")
        verify(exactly = 2) { hapticProvider.vibrate() }
        verify(exactly = 2) { audioProvider.playTone() }
    }

    @Test
    fun `feedback fires again for a word after it leaves and re-enters frame`() {
        manager.onWordDetected("exit")
        manager.onWordsCleared()         // word left the frame
        manager.onWordDetected("exit")   // word re-enters
        verify(exactly = 2) { hapticProvider.vibrate() }
    }

    // -------------------------------------------------------------------------
    // onWordsCleared resets state
    // -------------------------------------------------------------------------

    @Test
    fun `clearing words resets the seen set`() {
        manager.onWordDetected("exit")
        manager.onWordsCleared()
        manager.onWordDetected("exit")
        verify(exactly = 2) { hapticProvider.vibrate() }
    }

    @Test
    fun `clearing words with nothing detected does not throw`() {
        manager.onWordsCleared() // should be a no-op
    }
}