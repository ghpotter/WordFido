package com.gregoryhpotter.textlistscanner.matcher

/**
 * Matches words detected by OCR against a user-defined word list.
 *
 * Rules:
 *  - Punctuation is always stripped before comparison
 *  - Case sensitivity is togglable (default: OFF = case-insensitive)
 *  - Whole-word matching is togglable (default: OFF = partial matching allowed)
 *
 * Returns the matched entries from the original word list (not the OCR text),
 * so callers always get back the canonical form of the matched word.
 */
class WordMatcher {

    /**
     * @param text        Raw OCR-detected string (may be a word, phrase, or sentence)
     * @param wordList    The user's list of words to watch for
     * @param caseSensitive Whether matching is case-sensitive (default: false)
     * @param wholeWord   Whether only whole-word matches count (default: false)
     * @return            Set of matched entries from [wordList]
     */
    fun findMatches(
        text: String,
        wordList: List<String>,
        caseSensitive: Boolean = false,
        wholeWord: Boolean = false
    ): Set<String> {
        if (text.isBlank() || wordList.isEmpty()) return emptySet()

        // Sanitize the word list — strip blanks and deduplicate
        val candidates = wordList
            .filter { it.isNotBlank() }
            .distinctBy { if (caseSensitive) it else it.lowercase() }

        // Tokenize the input text by splitting on whitespace, then strip
        // leading/trailing punctuation from each token
        val tokens = text.trim()
            .split(Regex("\\s+"))
            .map { it.stripPunctuation() }
            .filter { it.isNotEmpty() }

        val matches = mutableSetOf<String>()

        for (candidate in candidates) {
            val cleanCandidate = candidate.stripPunctuation()
            if (cleanCandidate.isBlank()) continue

            if (wholeWord) {
                // A whole-word match requires the candidate to equal a token exactly
                val matched = tokens.any { token ->
                    token.equals(cleanCandidate, ignoreCase = !caseSensitive)
                }
                if (matched) matches.add(candidate)
            } else {
                // A partial match requires the candidate to appear anywhere in
                // the stripped, (optionally lowercased) full text
                val haystack = tokens.joinToString(" ")
                    .let { if (caseSensitive) it else it.lowercase() }
                val needle = cleanCandidate
                    .let { if (caseSensitive) it else it.lowercase() }
                if (haystack.contains(needle)) matches.add(candidate)
            }
        }

        return matches
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Removes leading and trailing punctuation characters from a string.
     * Internal punctuation (e.g. hyphens in compound words) is preserved.
     */
    private fun String.stripPunctuation(): String =
        this.trimStart { it.isLeadingPunctuation() }
            .trimEnd { it.isTrailingPunctuation() }

    private fun Char.isLeadingPunctuation(): Boolean =
        this in setOf('"', '\'', '(', '[', '{', '\u2018', '\u201C')

    private fun Char.isTrailingPunctuation(): Boolean =
        this in setOf('.', ',', '!', '?', ':', ';', ')', ']', '}',
            '"', '\'', '\u2019', '\u201D')
}