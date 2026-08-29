package com.joasasso.minitoolbox.metrics

import com.joasasso.minitoolbox.BuildConfig

/**
 * Fuente única de verdad para la configuración de métricas.
 * Lee directamente de [BuildConfig], eliminando la dependencia de SharedPreferences
 * para evitar regresiones por "split-brain".
 */
object MetricsConfig {
    val endpoint: String = BuildConfig.METRICS_ENDPOINT
    /**
     * Indica si el sistema de métricas está configurado correctamente para subir datos.
     * La autenticación es exclusivamente por Firebase App Check desde la 1.3.2.
     * Ya no existe API key en el cliente.
     */
    val isConfigured: Boolean = endpoint.isNotBlank()
}
