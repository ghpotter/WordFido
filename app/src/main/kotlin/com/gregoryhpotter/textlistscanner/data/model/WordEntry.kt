package com.gregoryhpotter.textlistscanner.data.model

/**
 * Represents a single entry in the user's word watch list.
 *
 * @param text    The word to watch for (stored in original casing)
 * @param color   ARGB color used to highlight this word in the camera view
 * @param enabled Whether this word is currently active for matching
 */
data class WordEntry(
    val text: String,
    val color: Int,
    val enabled: Boolean = true
)