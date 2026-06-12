package com.gregoryhpotter.textlistscanner.ocr

import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.matcher.WordMatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OcrAnalyzerTest {

    private lateinit var wordMatcher: WordMatcher
    private lateinit var processor: OcrResultProcessor

    @Before
    fun setUp() {
        wordMatcher = mockk()
        processor = OcrResultProcessor(wordMatcher)
    }

    // -------------------------------------------------------------------------
    // Basic matching
    // -------------------------------------------------------------------------

    @Test
    fun `process returns matched word entries from detected lines`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(OcrLine("Please use the exit", boundingBox = null)),
            wordList = wordList
        )

        assertEquals(1, result.size)
        assertEquals("exit", result.first().entry.text)
    }

    @Test
    fun `process returns empty when no matches found`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns emptySet()

        val result = processor.process(
            lines = listOf(OcrLine("The door is open", boundingBox = null)),
            wordList = wordList
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `process returns empty when word list is empty`() {
        val result = processor.process(
            lines = listOf(OcrLine("Please use the exit", boundingBox = null)),
            wordList = emptyList()
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `process returns empty when lines are empty`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        val result = processor.process(lines = emptyList(), wordList = wordList)
        assertTrue(result.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Bounding box is carried through to OcrMatch
    // -------------------------------------------------------------------------

    @Test
    fun `matched result carries bounding box from OcrLine`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val box = OcrBoundingBox(10, 20, 80, 40)
        val result = processor.process(
            lines = listOf(OcrLine("exit", boundingBox = box)),
            wordList = wordList
        )

        assertNotNull(result.first().boundingBox)
        assertEquals(box, result.first().boundingBox)
    }

    @Test
    fun `matched result uses line corner points when element box has none`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val lineBox = OcrBoundingBox(
            left = 0,
            top = 0,
            right = 100,
            bottom = 20,
            cornerPoints = listOf(
                OcrPoint(0, 0),
                OcrPoint(100, 0),
                OcrPoint(100, 20),
                OcrPoint(0, 20)
            )
        )
        val elementBox = OcrBoundingBox(
            left = 10,
            top = 5,
            right = 30,
            bottom = 15,
            cornerPoints = emptyList()
        )

        val result = processor.process(
            lines = listOf(
                OcrLine(
                    text = "exit",
                    boundingBox = lineBox,
                    elements = listOf(OcrElement(text = "exit", boundingBox = elementBox))
                )
            ),
            wordList = wordList
        )

        assertNotNull(result.first().boundingBox)
        assertEquals(lineBox.cornerPoints, result.first().boundingBox!!.cornerPoints)
    }

    @Test
    fun `matched result has null bounding box when line has no box`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(OcrLine("exit", boundingBox = null)),
            wordList = wordList
        )

        assertNull(result.first().boundingBox)
    }

    // -------------------------------------------------------------------------
    // Disabled entries excluded
    // -------------------------------------------------------------------------

    @Test
    fun `disabled word entries are excluded from matching`() {
        val wordList = listOf(
            WordEntry("exit", 0xFF0000, enabled = false),
            WordEntry("door", 0x00FF00, enabled = true)
        )
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns emptySet()

        processor.process(
            lines = listOf(OcrLine("Use the exit or door", boundingBox = null)),
            wordList = wordList
        )

        verify {
            wordMatcher.findMatches(
                any(),
                match { it.none { entry -> entry == "exit" } },
                any(),
                any()
            )
        }
    }

    // -------------------------------------------------------------------------
    // Multiple lines
    // -------------------------------------------------------------------------

    @Test
    fun `process handles multiple detected lines`() {
        val wordList = listOf(
            WordEntry("exit", 0xFF0000, true),
            WordEntry("door", 0x00FF00, true)
        )
        every { wordMatcher.findMatches("Use the exit", any(), any(), any()) } returns setOf("exit")
        every { wordMatcher.findMatches("Near the door", any(), any(), any()) } returns setOf("door")

        val result = processor.process(
            lines = listOf(
                OcrLine("Use the exit", boundingBox = null),
                OcrLine("Near the door", boundingBox = null)
            ),
            wordList = wordList
        )

        assertEquals(2, result.size)
        val words = result.map { it.entry.text }.toSet()
        assertTrue(words.contains("exit"))
        assertTrue(words.contains("door"))
    }

    @Test
    fun `same word matched on multiple lines is deduplicated`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(
                OcrLine("exit here", boundingBox = null),
                OcrLine("exit there", boundingBox = null)
            ),
            wordList = wordList
        )

        assertEquals(2, result.size)
    }

    // -------------------------------------------------------------------------
    // Settings forwarded correctly
    // -------------------------------------------------------------------------

    @Test
    fun `process forwards case sensitivity setting to matcher`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns emptySet()

        processor.process(
            lines = listOf(OcrLine("EXIT", boundingBox = null)),
            wordList = wordList,
            caseSensitive = true
        )

        verify { wordMatcher.findMatches(any(), any(), caseSensitive = true, any()) }
    }

    @Test
    fun `process forwards whole word setting to matcher`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns emptySet()

        processor.process(
            lines = listOf(OcrLine("exiting", boundingBox = null)),
            wordList = wordList,
            wholeWord = false
        )

        verify { wordMatcher.findMatches(any(), any(), any(), wholeWord = false) }
    }

    // -------------------------------------------------------------------------
    // Match metadata
    // -------------------------------------------------------------------------

    @Test
    fun `matched result carries correct color from word entry`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(OcrLine("exit", boundingBox = null)),
            wordList = wordList
        )

        assertEquals(0xFF0000, result.first().entry.color)
    }

    // -------------------------------------------------------------------------
    // Confidence filtering
    // -------------------------------------------------------------------------

    @Test
    fun `element above confidence threshold is reported`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(
                OcrLine(
                    text = "exit",
                    boundingBox = null,
                    elements = listOf(OcrElement("exit", null, confidence = 0.8f))
                )
            ),
            wordList = wordList
        )

        assertEquals(1, result.size)
    }

    @Test
    fun `element below confidence threshold is dropped`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(
                OcrLine(
                    text = "exit",
                    boundingBox = null,
                    elements = listOf(OcrElement("exit", null, confidence = 0.3f))
                )
            ),
            wordList = wordList
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `element at exactly the confidence threshold is reported`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(
                OcrLine(
                    text = "exit",
                    boundingBox = null,
                    elements = listOf(OcrElement("exit", null, confidence = OcrResultProcessor.MIN_CONFIDENCE))
                )
            ),
            wordList = wordList
        )

        assertEquals(1, result.size)
    }

    @Test
    fun `element with null confidence is reported`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(
                OcrLine(
                    text = "exit",
                    boundingBox = null,
                    elements = listOf(OcrElement("exit", null, confidence = null))
                )
            ),
            wordList = wordList
        )

        assertEquals(1, result.size)
    }

    @Test
    fun `only high-confidence elements reported when line has mixed confidence`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(
                OcrLine(
                    text = "exit exit",
                    boundingBox = null,
                    elements = listOf(
                        OcrElement("exit", null, confidence = 0.9f),
                        OcrElement("exit", null, confidence = 0.2f)
                    )
                )
            ),
            wordList = wordList
        )

        assertEquals(1, result.size)
    }

    @Test
    fun `match with no elements falls through to line bounding box without confidence check`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches(any(), any(), any(), any()) } returns setOf("exit")

        val lineBox = OcrBoundingBox(0, 0, 100, 20)
        val result = processor.process(
            lines = listOf(OcrLine("exit", boundingBox = lineBox, elements = emptyList())),
            wordList = wordList
        )

        assertEquals(1, result.size)
        assertEquals(lineBox, result.first().boundingBox)
    }

    @Test
    fun `matched result carries the source line text`() {
        val wordList = listOf(WordEntry("exit", 0xFF0000, true))
        every { wordMatcher.findMatches("use the exit", any(), any(), any()) } returns setOf("exit")

        val result = processor.process(
            lines = listOf(OcrLine("use the exit", boundingBox = null)),
            wordList = wordList
        )

        assertEquals("use the exit", result.first().sourceLine)
    }
}