package com.joasasso.minitoolbox

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class MiniToolboxApp : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig(): CameraXConfig {
        // Backend por defecto basado en Camera2
        return Camera2Config.defaultConfig()
    }
}
