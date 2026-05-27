package com.gregoryhpotter.textlistscanner.ocr

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.gregoryhpotter.textlistscanner.data.model.WordEntry

/**
 * CameraX [ImageAnalysis.Analyzer] that feeds camera frames to ML Kit
 * and forwards detected lines (with bounding boxes) to [OcrResultProcessor].
 */
class MlKitTextAnalyzer(
    private val processor: OcrResultProcessor,
    private val onResults: (List<OcrMatch>) -> Unit,
    private val wordListProvider: () -> List<WordEntry>,
    private var caseSensitive: Boolean = false,
    private var wholeWord: Boolean = false,
    private val onDebug: ((String) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val imageWidth = imageProxy.width
        val imageHeight = imageProxy.height
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Convert ML Kit lines into our plain OcrLine type,
                // mapping android.graphics.Rect → OcrBoundingBox
                val lines = visionText.textBlocks
                    .flatMap { block -> block.lines }
                    .map { line ->
                        OcrLine(
                            text = line.text,
                            boundingBox = line.boundingBox?.let { rect ->
                                OcrBoundingBox(
                                    left = rect.left,
                                    top = rect.top,
                                    right = rect.right,
                                    bottom = rect.bottom,
                                    cornerPoints = line.cornerPoints?.map { OcrPoint(it.x, it.y) } ?: emptyList()
                                )
                            },
                            elements = line.elements.map { element ->
                                OcrElement(
                                    text = element.text,
                                    boundingBox = element.boundingBox?.let { rect ->
                                        OcrBoundingBox(
                                            left = rect.left,
                                            top = rect.top,
                                            right = rect.right,
                                            bottom = rect.bottom,
                                            cornerPoints = element.cornerPoints?.map { OcrPoint(it.x, it.y) } ?: emptyList()
                                        )
                                    }
                                )
                            }
                        )
                    }

                val matches = processor.process(
                    lines = lines,
                    wordList = wordListProvider(),
                    caseSensitive = caseSensitive,
                    wholeWord = wholeWord
                ).map { match ->
                    match.copy(
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        rotationDegrees = rotationDegrees
                    )
                }


                onResults(matches)

                // Debug: If text detected but no matches, notify
                if (lines.isNotEmpty() && matches.isEmpty()) {
                    onDebug?.invoke("Detected ${lines.size} text lines but no matches")
                }
            }
            .addOnFailureListener {
                // Clear matches if this frame cannot be analyzed.
                onResults(emptyList())
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun updateSettings(caseSensitive: Boolean, wholeWord: Boolean) {
        this.caseSensitive = caseSensitive
        this.wholeWord = wholeWord
    }

    fun shutdown() {
        recognizer.close()
    }
}