package com.gregoryhpotter.textlistscanner.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.gregoryhpotter.textlistscanner.ui.camera.MapperRect
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

/**
 * Transparent overlay drawn on top of [androidx.camera.view.PreviewView].
 *
 * Renders a highlight rect for each active [HighlightData]. The highlight
 * style is pluggable via [HighlightStyle].
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Current highlights to draw — set via [updateHighlights]
    private var highlights: List<HighlightData> = emptyList()

    // Current style — can be swapped at runtime
    var style: HighlightStyle = HighlightStyle.Marker

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = LABEL_TEXT_SIZE
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    /**
     * Update the highlights and trigger a redraw.
     * Safe to call from any thread — posts invalidation to the UI thread.
     */
    fun updateHighlights(newHighlights: List<HighlightData>) {
        highlights = newHighlights
        postInvalidate()
    }

    fun clearHighlights() {
        highlights = emptyList()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        highlights.forEach { highlight ->
            if (highlight.rect.cornerPoints.isNotEmpty()) {
                drawAlignedHighlight(canvas, highlight)
            } else {
                when (style) {
                    HighlightStyle.BoundingBox -> drawBoundingBox(canvas, highlight)
                    HighlightStyle.Marker -> drawMarker(canvas, highlight)
                }
            }
            drawLabel(canvas, highlight)
        }
    }

    // -------------------------------------------------------------------------
    // Drawing implementations
    // -------------------------------------------------------------------------

    private fun drawAlignedHighlight(canvas: Canvas, highlight: HighlightData) {
        val path = android.graphics.Path().apply {
            val points = highlight.rect.cornerPoints
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            close()
        }

        paint.apply {
            color = if (this@OverlayView.style == HighlightStyle.Marker) {
                ColorUtils.setAlphaComponent(highlight.color, MARKER_ALPHA)
            } else {
                highlight.color
            }
            style = if (this@OverlayView.style == HighlightStyle.Marker) Paint.Style.FILL else Paint.Style.STROKE
            if (style == Paint.Style.STROKE) {
                strokeWidth = STROKE_WIDTH
                alpha = 220
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawBoundingBox(canvas: Canvas, highlight: HighlightData) {
        paint.apply {
            style = Paint.Style.STROKE
            color = highlight.color
            strokeWidth = STROKE_WIDTH
            alpha = 220
        }
        canvas.drawRoundRect(highlight.rect.toRectF(), CORNER_RADIUS, CORNER_RADIUS, paint)
    }

    private fun drawMarker(canvas: Canvas, highlight: HighlightData) {
        // Fill with a semi-transparent version of the highlight color
        paint.apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(highlight.color, MARKER_ALPHA)
        }
        canvas.drawRoundRect(highlight.rect.toRectF(), CORNER_RADIUS, CORNER_RADIUS, paint)
    }

    private fun drawLabel(canvas: Canvas, highlight: HighlightData) {
        if (highlight.label.isBlank()) return
        labelPaint.color = highlight.color
        labelPaint.alpha = 220
        canvas.drawText(highlight.label, highlight.rect.left, highlight.rect.top - LABEL_MARGIN, labelPaint)
    }

    companion object {
        private const val STROKE_WIDTH = 4f
        private const val CORNER_RADIUS = 6f
        private const val MARKER_ALPHA = 80
        private const val LABEL_TEXT_SIZE = 32f
        private const val LABEL_MARGIN = 8f
    }
}

/**
 * Data needed to draw a single highlight rect.
 *
 * @param rect   Bounding box in [OverlayView] coordinate space
 * @param color  ARGB color from the matching [WordEntry]
 * @param label  The matched word text (reserved for future label drawing)
 */
data class HighlightData(
    val rect: MapperRect,
    val color: Int,
    val label: String
)

/**
 * Pluggable highlight rendering styles.
 * Add new entries here as new styles are designed.
 */
sealed class HighlightStyle {
    /** Colored rectangle border around the word */
    object BoundingBox : HighlightStyle()

    /** Semi-transparent filled rectangle over the word */
    object Marker : HighlightStyle()
}

/**
 * Converts a [MapperRect] to an [android.graphics.RectF] for drawing.
 * Only called from the View layer where Android classes are available.
 */
fun MapperRect.toRectF(): RectF = RectF(left, top, right, bottom)