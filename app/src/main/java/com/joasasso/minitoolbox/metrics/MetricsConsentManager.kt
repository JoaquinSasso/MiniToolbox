// metrics/ConsentManager.kt
package com.joasasso.minitoolbox.metrics

import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository

class ConsentManager(private val aggregates: AggregatesRepository) {

    suspend fun setEnabled(enabled: Boolean) {
        aggregates.setMetricsConsent(enabled)
    }

    suspend fun isEnabled(): Boolean {
        return aggregates.isMetricsConsentEnabled()
    }
}


