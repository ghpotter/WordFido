package com.gregoryhpotter.textlistscanner.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

/**
 * Abstracts haptic feedback so [FeedbackManager] can be unit tested
 * without Android hardware dependencies.
 */
interface HapticFeedbackProvider {
    fun vibrate()
}

/**
 * Production implementation using the Android [Vibrator] API.
 */
class AndroidHapticFeedbackProvider(private val context: Context) : HapticFeedbackProvider {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = ContextCompat.getSystemService(context, VibratorManager::class.java)
            manager?.defaultVibrator ?: fallbackVibrator()
        } else {
            fallbackVibrator()
        }
    }

    override fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    VIBRATION_DURATION_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_DURATION_MS)
        }
    }

    @Suppress("DEPRECATION")
    private fun fallbackVibrator(): Vibrator =
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    companion object {
        private const val VIBRATION_DURATION_MS = 80L
    }
}