package com.gregoryhpotter.textlistscanner.ocr

import com.gregoryhpotter.textlistscanner.data.model.WordEntry

/**
 * Represents a single successful match between OCR-detected text
 * and an entry in the user's word list.
 *
 * @param entry           The matched [WordEntry] from the word list
 * @param sourceLine      The raw OCR line text in which the match was found
 * @param boundingBox     Bounding box in camera image coordinates (null if unavailable)
 * @param imageWidth      Width of the camera image frame
 * @param imageHeight     Height of the camera image frame
 * @param rotationDegrees Rotation of the camera image relative to the screen
 */
data class OcrMatch(
    val entry: WordEntry,
    val sourceLine: String,
    val boundingBox: OcrBoundingBox? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val rotationDegrees: Int = 0
)