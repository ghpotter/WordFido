package com.gregoryhpotter.textlistscanner.ui.camera

import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo

class TorchController(
    private val cameraInfo: CameraInfo,
    private val cameraControl: CameraControl
) {
    val hasFlash: Boolean get() = cameraInfo.hasFlashUnit()

    var enabled: Boolean = false
        private set

    fun toggle() {
        enabled = !enabled
        cameraControl.enableTorch(enabled)
    }

    fun release() {
        if (enabled) {
            enabled = false
            cameraControl.enableTorch(false)
        }
    }
}
