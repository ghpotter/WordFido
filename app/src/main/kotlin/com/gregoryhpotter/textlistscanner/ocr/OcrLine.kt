package com.gregoryhpotter.textlistscanner.ocr

/**
 * Plain Kotlin point — avoids [android.graphics.Point] which is
 * a stub on the JVM.
 */
data class OcrPoint(val x: Int, val y: Int)

/**
 * Plain Kotlin bounding box — avoids [android.graphics.Rect] which is
 * a stub on the JVM and cannot be used in unit tests.
 *
 * Includes [cornerPoints] to support rotated text boxes.
 */
data class OcrBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val cornerPoints: List<OcrPoint> = emptyList()
)

/**
 * A single OCR text element (word-like chunk) with its bounding box.
 */
data class OcrElement(
        val text: String,
        val boundingBox: OcrBoundingBox?,
        val confidence: Float? = null
)

/**
 * A single line of text detected by ML Kit, paired with its bounding box.
 * Elements are the individual words or tokens within the line.
 * Using our own types keeps [OcrResultProcessor] free of Android/ML Kit deps.
 */
data class OcrLine(
        val text: String,
        val boundingBox: OcrBoundingBox?,
        val elements: List<OcrElement> = emptyList()
)