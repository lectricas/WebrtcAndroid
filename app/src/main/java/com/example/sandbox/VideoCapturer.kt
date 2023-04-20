package com.example.sandbox

import android.content.Context
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer

object VideoCapturer {
    fun getVideoCapture(context: Context): CameraVideoCapturer {
        val videoCapturer = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(false)
        }.run {
            deviceNames.find { deviceName ->
                isFrontFacing(deviceName)
            }?.let {
                createCapturer(it, null)
            }
        }
        return videoCapturer!!
    }
}