package com.joasasso.minitoolbox.metrics

import com.joasasso.minitoolbox.BuildConfig

/**
 * Fuente única de verdad para la configuración de métricas.
 * Lee directamente de [BuildConfig], eliminando la dependencia de SharedPreferences
 * para evitar regresiones por "split-brain".
 */
object MetricsConfig {
    val endpoint: String = BuildConfig.METRICS_ENDPOINT
    val apiKey: String = BuildConfig.METRICS_API_KEY

    /**
     * Indica si el sistema de métricas está configurado correctamente para subir datos.
     */
    val isConfigured: Boolean = endpoint.isNotBlank() && apiKey.isNotBlank()
}
