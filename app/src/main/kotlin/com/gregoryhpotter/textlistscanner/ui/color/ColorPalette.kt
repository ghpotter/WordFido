package com.gregoryhpotter.textlistscanner.ui.color

import androidx.annotation.ColorInt

object ColorPalette {

    @ColorInt
    val colors: List<Int> = listOf(
        0xFFE53935.toInt(), // Red
        0xFFE91E63.toInt(), // Pink
        0xFF9C27B0.toInt(), // Purple
        0xFF3F51B5.toInt(), // Indigo
        0xFF2196F3.toInt(), // Blue
        0xFF00BCD4.toInt(), // Cyan
        0xFF009688.toInt(), // Teal
        0xFF4CAF50.toInt(), // Green
        0xFFCDDC39.toInt(), // Lime
        0xFFFF9800.toInt(), // Orange
        0xFF795548.toInt(), // Brown
        0xFF607D8B.toInt(), // Blue Grey
    )

    /** Returns the first color in the palette — used as the default for new words. */
    @ColorInt
    fun default(): Int = colors.first()

    /** Returns the next color after [current], wrapping around. */
    @ColorInt
    fun next(current: Int): Int {
        val idx = colors.indexOf(current)
        return if (idx == -1 || idx == colors.lastIndex) colors.first()
        else colors[idx + 1]
    }
}