package com.joasasso.minitoolbox.utils

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.facebook.ads.AdSettings
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MiniToolboxApp : Application(), CameraXConfig.Provider {

    override fun onCreate() {
        super.onCreate()
        // --- AdMob: test devices ---
        val admobTestIds = listOf(
            "9B8C765995C4CA74CAA5FB846DED2F1A",
            AdRequest.DEVICE_ID_EMULATOR       // emulador
        )
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(admobTestIds)
                .build()
        )
        MobileAds.initialize(this)

        // --- Meta / Facebook Audience Network: test device ---
        AdSettings.addTestDevice("18b4c44d-f255-419d-908a-d278a6ec1105")
    }

    override fun getCameraXConfig(): CameraXConfig {
        // Backend por defecto basado en Camera2
        return Camera2Config.defaultConfig()
    }
}
