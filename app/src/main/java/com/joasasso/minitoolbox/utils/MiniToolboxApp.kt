package com.joasasso.minitoolbox.utils

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.facebook.ads.AdSettings
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.joasasso.minitoolbox.metrics.AppCheckSetup
import com.joasasso.minitoolbox.metrics.storage.MetricsSanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MiniToolboxApp : Application(), CameraXConfig.Provider {

    /**
     * Scope de proceso para tareas de mantenimiento que no deben atarse a
     * ninguna pantalla. SupervisorJob evita que un fallo cancele el resto.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // App Check: debe instalarse antes del primer envío de métricas
        AppCheckSetup.install(this)

        // Saneo del almacenamiento local de métricas. Corre una sola vez por
        // versión de esquema; en el resto de los arranques sale de inmediato.
        appScope.launch {
            MetricsSanitizer.runIfNeeded(this@MiniToolboxApp)
        }

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