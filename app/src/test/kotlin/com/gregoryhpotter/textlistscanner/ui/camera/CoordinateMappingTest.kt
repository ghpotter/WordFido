package com.gregoryhpotter.textlistscanner.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [CoordinateMapper].
 *
 * Uses [MapperRect] (plain Kotlin data class) instead of [android.graphics.RectF]
 * so these tests run on the JVM without Robolectric.
 */
class CoordinateMappingTest {

    private val mapper = CoordinateMapper()

    // -------------------------------------------------------------------------
    // Identity mapping — preview exactly matches image size
    // -------------------------------------------------------------------------

    @Test
    fun `rect maps correctly when preview matches image size exactly`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(10f, 10f, 50f, 30f),
            imageWidth = 100,
            imageHeight = 100,
            previewWidth = 100,
            previewHeight = 100
        )
        assertEquals(10f, result.left, 0.01f)
        assertEquals(10f, result.top, 0.01f)
        assertEquals(50f, result.right, 0.01f)
        assertEquals(30f, result.bottom, 0.01f)
    }

    // -------------------------------------------------------------------------
    // Uniform scaling
    // -------------------------------------------------------------------------

    @Test
    fun `rect scales correctly when preview is twice image size`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(10f, 10f, 50f, 30f),
            imageWidth = 100,
            imageHeight = 100,
            previewWidth = 200,
            previewHeight = 200
        )
        assertEquals(20f, result.left, 0.01f)
        assertEquals(20f, result.top, 0.01f)
        assertEquals(100f, result.right, 0.01f)
        assertEquals(60f, result.bottom, 0.01f)
    }

    @Test
    fun `rect scales correctly when preview is half image size`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(10f, 10f, 50f, 30f),
            imageWidth = 100,
            imageHeight = 100,
            previewWidth = 50,
            previewHeight = 50
        )
        assertEquals(5f, result.left, 0.01f)
        assertEquals(5f, result.top, 0.01f)
        assertEquals(25f, result.right, 0.01f)
        assertEquals(15f, result.bottom, 0.01f)
    }

    // -------------------------------------------------------------------------
    // Non-uniform scaling (different aspect ratios)
    // -------------------------------------------------------------------------

    @Test
    fun `rect maps correctly when aspect ratios differ — wider preview`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(25f, 25f, 75f, 75f),
            imageWidth = 100,
            imageHeight = 100,
            previewWidth = 200,
            previewHeight = 100
        )
        assertEquals(50f, result.left, 0.01f)
        assertEquals(0f, result.top, 0.01f)
        assertEquals(150f, result.right, 0.01f)
        assertEquals(100f, result.bottom, 0.01f)
    }

    @Test
    fun `rect maps correctly when aspect ratios differ — taller preview`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(25f, 25f, 75f, 75f),
            imageWidth = 100,
            imageHeight = 100,
            previewWidth = 100,
            previewHeight = 200
        )
        assertEquals(0f, result.left, 0.01f)
        assertEquals(50f, result.top, 0.01f)
        assertEquals(100f, result.right, 0.01f)
        assertEquals(150f, result.bottom, 0.01f)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `rect at image origin maps to preview origin`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(0f, 0f, 10f, 10f),
            imageWidth = 100,
            imageHeight = 100,
            previewWidth = 200,
            previewHeight = 200
        )
        assertEquals(0f, result.left, 0.01f)
        assertEquals(0f, result.top, 0.01f)
    }

    @Test
    fun `rect at image boundary maps to preview boundary`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(0f, 0f, 100f, 100f),
            imageWidth = 100,
            imageHeight = 100,
            previewWidth = 300,
            previewHeight = 150
        )
        assertEquals(0f, result.left, 0.01f)
        assertEquals(-75f, result.top, 0.01f)
        assertEquals(300f, result.right, 0.01f)
        assertEquals(225f, result.bottom, 0.01f)
    }

    @Test
    fun `very small bounding box is preserved`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(50f, 50f, 51f, 51f),
            imageWidth = 100,
            imageHeight = 100,
            previewWidth = 100,
            previewHeight = 100
        )
        assertEquals(50f, result.left, 0.01f)
        assertEquals(50f, result.top, 0.01f)
        assertEquals(51f, result.right, 0.01f)
        assertEquals(51f, result.bottom, 0.01f)
    }

    // -------------------------------------------------------------------------
    // Rotation
    // -------------------------------------------------------------------------

    @Test
    fun `rect maps correctly with 90 degree rotation`() {
        val result = mapper.mapToPreview(
            boundingBox = MapperRect(0f, 0f, 640f, 480f),
            imageWidth = 640,
            imageHeight = 480,
            previewWidth = 480,
            previewHeight = 640,
            rotationDegrees = 90
        )
        assertEquals(0f, result.left, 0.01f)
        assertEquals(0f, result.top, 0.01f)
        assertEquals(480f, result.right, 0.01f)
        assertEquals(640f, result.bottom, 0.01f)
    }

    @Test
    fun `rect maps correctly with 45 degree rotation using corner points`() {
        val boundingBox = MapperRect(
            left = 0f,
            top = 0f,
            right = 10f,
            bottom = 10f,
            cornerPoints = listOf(
                MapperPoint(0f, 0f),
                MapperPoint(10f, 0f),
                MapperPoint(10f, 10f),
                MapperPoint(0f, 10f)
            )
        )

        val result = mapper.mapToPreview(
            boundingBox = boundingBox,
            imageWidth = 10,
            imageHeight = 10,
            previewWidth = 10,
            previewHeight = 10,
            rotationDegrees = 45
        )

        assertEquals(0f, result.left, 0.01f)
        assertEquals(0f, result.top, 0.01f)
        assertEquals(10f, result.right, 0.01f)
        assertEquals(10f, result.bottom, 0.01f)
        assertEquals(4, result.cornerPoints.size)
        assertEquals(5f, result.cornerPoints[0].x, 0.01f)
        assertEquals(0f, result.cornerPoints[0].y, 0.01f)
        assertEquals(10f, result.cornerPoints[1].x, 0.01f)
        assertEquals(5f, result.cornerPoints[1].y, 0.01f)
        assertEquals(5f, result.cornerPoints[2].x, 0.01f)
        assertEquals(10f, result.cornerPoints[2].y, 0.01f)
        assertEquals(0f, result.cornerPoints[3].x, 0.01f)
        assertEquals(5f, result.cornerPoints[3].y, 0.01f)
    }

    @Test
    fun `rect maps correctly with 60 degree rotation using corner points`() {
        val boundingBox = MapperRect(
            left = 0f,
            top = 0f,
            right = 10f,
            bottom = 10f,
            cornerPoints = listOf(
                MapperPoint(0f, 0f),
                MapperPoint(10f, 0f),
                MapperPoint(10f, 10f),
                MapperPoint(0f, 10f)
            )
        )

        val result = mapper.mapToPreview(
            boundingBox = boundingBox,
            imageWidth = 10,
            imageHeight = 10,
            previewWidth = 10,
            previewHeight = 10,
            rotationDegrees = 60
        )

        assertEquals(0f, result.left, 0.01f)
        assertEquals(0f, result.top, 0.01f)
        assertEquals(10f, result.right, 0.01f)
        assertEquals(10f, result.bottom, 0.01f)
        assertEquals(4, result.cornerPoints.size)
        assertEquals(6.34f, result.cornerPoints[0].x, 0.02f)
        assertEquals(0f, result.cornerPoints[0].y, 0.02f)
        assertEquals(10f, result.cornerPoints[1].x, 0.02f)
        assertEquals(6.34f, result.cornerPoints[1].y, 0.02f)
        assertEquals(3.66f, result.cornerPoints[2].x, 0.02f)
        assertEquals(10f, result.cornerPoints[2].y, 0.02f)
        assertEquals(0f, result.cornerPoints[3].x, 0.02f)
        assertEquals(3.66f, result.cornerPoints[3].y, 0.02f)
    }
}
