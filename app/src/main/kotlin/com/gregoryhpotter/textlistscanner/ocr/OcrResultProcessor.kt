package com.gregoryhpotter.textlistscanner.ocr

import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.matcher.WordMatcher
import javax.inject.Inject

/**
 * Pure logic layer that processes raw OCR lines against the word list.
 *
 * Accepts [OcrLine] objects (our own plain Kotlin type) so this class
 * has no ML Kit or Android dependencies and is fully unit testable on the JVM.
 */
class OcrResultProcessor @Inject constructor(
    private val wordMatcher: WordMatcher
) {

    /**
     * @param lines          Lines detected by ML Kit, each with optional bounding box
     * @param wordList       Full word list — disabled entries are filtered out
     * @param caseSensitive  Forwarded to [WordMatcher]
     * @param wholeWord      Forwarded to [WordMatcher]
     * @return               List of [OcrMatch] results for every matching word occurrence
     */
    fun process(
        lines: List<OcrLine>,
        wordList: List<WordEntry>,
        caseSensitive: Boolean = false,
        wholeWord: Boolean = false
    ): List<OcrMatch> {
        if (lines.isEmpty() || wordList.isEmpty()) return emptyList()

        val activeEntries = wordList.filter { it.enabled }
        if (activeEntries.isEmpty()) return emptyList()

        val activeTexts = activeEntries.map { it.text }
        val results = mutableListOf<OcrMatch>()

        for (line in lines) {
            val matches = wordMatcher.findMatches(
                text = line.text,
                wordList = activeTexts,
                caseSensitive = caseSensitive,
                wholeWord = wholeWord
            )

            for (matchedText in matches) {
                val entry = activeEntries.first {
                    it.text.equals(matchedText, ignoreCase = !caseSensitive)
                }

                val matchingElements = line.elements.filter { element ->
                    element.text.stripPunctuation().equals(matchedText.stripPunctuation(), ignoreCase = !caseSensitive)
                }

                if (matchingElements.isNotEmpty()) {
                    matchingElements
                        .filter { it.confidence == null || it.confidence >= MIN_CONFIDENCE }
                        .forEach { element ->
                            results.add(
                                OcrMatch(
                                    entry = entry,
                                    sourceLine = line.text,
                                    boundingBox = element.boundingBox?.withFallbackCornerPoints(line.boundingBox)
                                        ?: line.boundingBox
                                )
                            )
                        }
                } else {
                    results.add(
                        OcrMatch(
                            entry = entry,
                            sourceLine = line.text,
                            boundingBox = line.boundingBox
                        )
                    )
                }
            }
        }

        return results
    }

    companion object {
        const val MIN_CONFIDENCE = 0.5f
    }

    private fun OcrBoundingBox.withFallbackCornerPoints(fallback: OcrBoundingBox?): OcrBoundingBox {
        if (cornerPoints.isNotEmpty() || fallback?.cornerPoints.isNullOrEmpty()) return this
        return copy(cornerPoints = fallback.cornerPoints)
    }

    private fun String.stripPunctuation(): String =
        this.trimStart { it.isLeadingPunctuation() }
            .trimEnd { it.isTrailingPunctuation() }

    private fun Char.isLeadingPunctuation(): Boolean =
        this in setOf('"', '\'', '(', '[', '{', '\u2018', '\u201C')

    private fun Char.isTrailingPunctuation(): Boolean =
        this in setOf('.', ',', '!', '?', ':', ';', ')', ']', '}',
            '"', '\'', '\u2019', '\u201D')
}