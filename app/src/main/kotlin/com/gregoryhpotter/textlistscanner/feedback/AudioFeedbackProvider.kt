package com.gregoryhpotter.textlistscanner.feedback

import android.media.AudioManager
import android.media.ToneGenerator
import com.gregoryhpotter.textlistscanner.R

enum class AudioFeedbackTone(val toneType: Int, val labelResId: Int) {
    Beep(ToneGenerator.TONE_PROP_BEEP, R.string.audio_tone_beep),
    Prompt(ToneGenerator.TONE_PROP_PROMPT, R.string.audio_tone_prompt),
    Ack(ToneGenerator.TONE_PROP_ACK, R.string.audio_tone_ack)
}

interface AudioFeedbackProvider {
    fun playTone()
    fun setTone(tone: AudioFeedbackTone)
    fun release()
}

class AndroidAudioFeedbackProvider : AudioFeedbackProvider {

    private var currentTone: AudioFeedbackTone = AudioFeedbackTone.Beep

    // Not lazy — recreated if released
    private var toneGenerator: ToneGenerator? = createToneGenerator()

    private fun createToneGenerator(): ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME_PERCENT)
    } catch (e: RuntimeException) {
        null
    }

    override fun playTone() {
        // Recreate if previously released
        if (toneGenerator == null) {
            toneGenerator = createToneGenerator()
        }
        try {
            toneGenerator?.startTone(currentTone.toneType, TONE_DURATION_MS)
        } catch (e: RuntimeException) {
            // ToneGenerator was released externally — recreate on next call
            toneGenerator = null
        }
    }

    override fun setTone(tone: AudioFeedbackTone) {
        currentTone = tone
    }

    override fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        private const val VOLUME_PERCENT = 60
        private const val TONE_DURATION_MS = 150
    }
}