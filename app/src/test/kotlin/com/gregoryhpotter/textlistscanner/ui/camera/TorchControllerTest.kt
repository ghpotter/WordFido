package com.gregoryhpotter.textlistscanner.ui.camera

import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TorchControllerTest {

    private lateinit var cameraInfo: CameraInfo
    private lateinit var cameraControl: CameraControl
    private lateinit var controller: TorchController

    @Before
    fun setUp() {
        cameraInfo = mockk { every { hasFlashUnit() } returns true }
        cameraControl = mockk(relaxed = true)
        controller = TorchController(cameraInfo, cameraControl)
    }

    // -------------------------------------------------------------------------
    // hasFlash
    // -------------------------------------------------------------------------

    @Test
    fun `hasFlash delegates to cameraInfo`() {
        assertTrue(controller.hasFlash)
    }

    @Test
    fun `hasFlash is false when device has no flash unit`() {
        every { cameraInfo.hasFlashUnit() } returns false
        assertFalse(controller.hasFlash)
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `torch is off by default`() {
        assertFalse(controller.enabled)
    }

    // -------------------------------------------------------------------------
    // toggle
    // -------------------------------------------------------------------------

    @Test
    fun `toggle turns torch on`() {
        controller.toggle()
        assertTrue(controller.enabled)
    }

    @Test
    fun `toggle twice turns torch off again`() {
        controller.toggle()
        controller.toggle()
        assertFalse(controller.enabled)
    }

    @Test
    fun `toggle calls enableTorch true on first call`() {
        controller.toggle()
        verify { cameraControl.enableTorch(true) }
    }

    @Test
    fun `toggle calls enableTorch false on second call`() {
        controller.toggle()
        controller.toggle()
        verify { cameraControl.enableTorch(false) }
    }

    // -------------------------------------------------------------------------
    // release
    // -------------------------------------------------------------------------

    @Test
    fun `release turns torch off when it was on`() {
        controller.toggle()
        controller.release()
        assertFalse(controller.enabled)
    }

    @Test
    fun `release calls enableTorch false when torch was on`() {
        controller.toggle()
        controller.release()
        verify { cameraControl.enableTorch(false) }
    }

    @Test
    fun `release is a no-op when torch is already off`() {
        controller.release()
        verify(exactly = 0) { cameraControl.enableTorch(any()) }
    }

    @Test
    fun `release after release does not call enableTorch twice`() {
        controller.toggle()
        controller.release()
        controller.release()
        verify(exactly = 1) { cameraControl.enableTorch(false) }
    }
}
