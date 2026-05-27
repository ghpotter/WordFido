package com.gregoryhpotter.textlistscanner.ui.color

import org.junit.Assert.*
import org.junit.Test

class ColorPaletteTest {

    @Test
    fun `palette has exactly 12 colors`() {
        assertEquals(12, ColorPalette.colors.size)
    }

    @Test
    fun `all colors are fully opaque`() {
        ColorPalette.colors.forEach { color ->
            val alpha = (color ushr 24) and 0xFF
            assertEquals("Color $color should be fully opaque", 255, alpha)
        }
    }

    @Test
    fun `default returns first color`() {
        assertEquals(ColorPalette.colors.first(), ColorPalette.default())
    }

    @Test
    fun `next wraps from last color to first`() {
        val last = ColorPalette.colors.last()
        assertEquals(ColorPalette.colors.first(), ColorPalette.next(last))
    }

    @Test
    fun `next returns second color when given first`() {
        assertEquals(ColorPalette.colors[1], ColorPalette.next(ColorPalette.colors[0]))
    }

    @Test
    fun `next returns first color for unknown color`() {
        assertEquals(ColorPalette.colors.first(), ColorPalette.next(0x00000000))
    }

    @Test
    fun `all colors are distinct`() {
        assertEquals(ColorPalette.colors.size, ColorPalette.colors.toSet().size)
    }
}