package com.joasasso.minitoolbox.utils

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
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

        // Inicializar canal de recordatorio de agua y programar reset diario a medianoche
        com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.createWaterReminderChannel(this)
        com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.programarResetAguaDiario(this)
    }

    override fun getCameraXConfig(): CameraXConfig {
        // Backend por defecto basado en Camera2
        return Camera2Config.defaultConfig()
    }
}