package com.gregoryhpotter.textlistscanner.ocr

class FrameThrottle(
    val thresholdMs: Long = DEFAULT_THRESHOLD_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var lastProcessedMs: Long = -1L

    fun shouldProcess(): Boolean {
        val now = clock()
        return if (lastProcessedMs < 0L || now - lastProcessedMs >= thresholdMs) {
            lastProcessedMs = now
            true
        } else {
            false
        }
    }

    companion object {
        const val DEFAULT_THRESHOLD_MS = 300L
    }
}
