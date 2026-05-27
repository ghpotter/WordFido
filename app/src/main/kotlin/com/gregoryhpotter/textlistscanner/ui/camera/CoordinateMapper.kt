package com.gregoryhpotter.textlistscanner.ui.camera

import kotlin.math.abs

data class MapperPoint(val x: Float, val y: Float)

/**
 * Plain Kotlin rect used in coordinate mapping — avoids Android's [android.graphics.RectF]
 * which is a stub on the JVM and cannot be used in unit tests.
 *
 * Convert to [android.graphics.RectF] only at the drawing layer via [toRectF].
 */
data class MapperRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val cornerPoints: List<MapperPoint> = emptyList()
)

/**
 * Maps bounding boxes from ML Kit's camera image coordinate space
 * to the screen coordinate space of the [androidx.camera.view.PreviewView].
 *
 * Uses [MapperRect] instead of [android.graphics.RectF] so this class
 * is fully testable on the JVM without Robolectric.
 */
class CoordinateMapper {

    /**
     * Maps a single bounding box from image space to preview space.
     *
     * @param boundingBox     Rect in camera image coordinates
     * @param imageWidth      Camera image width in pixels
     * @param imageHeight     Camera image height in pixels
     * @param previewWidth    Preview view width in pixels
     * @param previewHeight   Preview view height in pixels
     * @param rotationDegrees Clockwise rotation of the image relative to preview
     * @return                Rect in preview view coordinates
     */
    fun mapToPreview(
        boundingBox: MapperRect,
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Int,
        previewHeight: Int,
        rotationDegrees: Int = 0
    ): MapperRect {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val angleRadians = Math.toRadians(normalizedRotation.toDouble())
        val cos = kotlin.math.cos(angleRadians).toFloat()
        val sin = kotlin.math.sin(angleRadians).toFloat()
        val absCos = abs(cos)
        val absSin = abs(sin)

        val rotatedImageWidth = imageWidth * absCos + imageHeight * absSin
        val rotatedImageHeight = imageWidth * absSin + imageHeight * absCos

        // PreviewView is configured to fill its bounds, so the rotated image may
        // be center-cropped. Use a uniform scale and apply the crop offset.
        val scale = maxOf(
            previewWidth.toFloat() / rotatedImageWidth,
            previewHeight.toFloat() / rotatedImageHeight
        )
        val scaledImageWidth = rotatedImageWidth * scale
        val scaledImageHeight = rotatedImageHeight * scale
        val offsetX = (scaledImageWidth - previewWidth) / 2f
        val offsetY = (scaledImageHeight - previewHeight) / 2f

        val imageCenterX = imageWidth / 2f
        val imageCenterY = imageHeight / 2f
        val rotatedCenterX = rotatedImageWidth / 2f
        val rotatedCenterY = rotatedImageHeight / 2f

        fun mapPoint(x: Float, y: Float): MapperPoint {
            val dx = x - imageCenterX
            val dy = y - imageCenterY
            val rotatedX = dx * cos - dy * sin
            val rotatedY = dx * sin + dy * cos

            return MapperPoint(
                x = (rotatedX + rotatedCenterX) * scale - offsetX,
                y = (rotatedY + rotatedCenterY) * scale - offsetY
            )
        }

        val sourcePoints = if (boundingBox.cornerPoints.isNotEmpty()) {
            boundingBox.cornerPoints
        } else {
            listOf(
                MapperPoint(boundingBox.left, boundingBox.top),
                MapperPoint(boundingBox.right, boundingBox.top),
                MapperPoint(boundingBox.right, boundingBox.bottom),
                MapperPoint(boundingBox.left, boundingBox.bottom)
            )
        }

        val mappedCornerPoints = sourcePoints.map { mapPoint(it.x, it.y) }
        val left = mappedCornerPoints.minOf { it.x }
        val top = mappedCornerPoints.minOf { it.y }
        val right = mappedCornerPoints.maxOf { it.x }
        val bottom = mappedCornerPoints.maxOf { it.y }

        return MapperRect(left, top, right, bottom, cornerPoints = mappedCornerPoints)
    }
}