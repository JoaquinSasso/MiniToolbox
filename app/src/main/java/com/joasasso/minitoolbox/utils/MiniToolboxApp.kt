package com.joasasso.minitoolbox.utils

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.google.android.gms.ads.MobileAds
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.storage.IdentityRepository

class MiniToolboxApp : Application(), CameraXConfig.Provider {

    override fun onCreate() {
        super.onCreate()
        // Init AdMob una sola vez
        MobileAds.initialize(this)

        val aggregates = AggregatesRepository(this)
        val identityRepo = IdentityRepository(this)

    // generar identidad anónima si hace falta
    // (hash disponible para adjuntar como prop común a eventos)
        val identity = kotlinx.coroutines.runBlocking { identityRepo.ensureIdentity() }

    // marcar instalación si es la primera vez
        kotlinx.coroutines.runBlocking { aggregates.markInstallIfNeeded() }

    }

    override fun getCameraXConfig(): CameraXConfig {
        // Backend por defecto basado en Camera2
        return Camera2Config.defaultConfig()
    }
}