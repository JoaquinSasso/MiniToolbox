package com.joasasso.minitoolbox.metrics.uploader

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import kotlinx.coroutines.flow.first
import java.time.Duration

object UploadScheduler {
    private const val SP_NAME = "metrics_upload_prefs"
    private const val KEY_DIRTY = "dirty"
    private const val KEY_LAST_ENQUEUED_MS = "last_enqueued_ms"

    private const val MIN_INTERVAL_MS = 3 * 60 * 60 * 1000L // 3h ventana mínima para maybeSchedule
    internal val BACKOFF: Duration = Duration.ofMinutes(15)
    private val INITIAL_DELAY = Duration.ofSeconds(20)

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    fun markDirty(ctx: Context) { prefs(ctx).edit { putBoolean(KEY_DIRTY, true) } }
    fun clearDirty(ctx: Context) { prefs(ctx).edit { putBoolean(KEY_DIRTY, false) } }
    fun isDirty(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DIRTY, false)
    fun lastEnqueuedMs(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST_ENQUEUED_MS, 0L)

    /** Programación “lenta” respetando ventana mínima, batería, etc. */
    fun maybeSchedule(ctx: Context, endpoint: String, apiKey: String) {
        if (endpoint.isBlank()) return
        val p = prefs(ctx)
        if (!p.getBoolean(KEY_DIRTY, false)) return

        val now = System.currentTimeMillis()
        val last = p.getLong(KEY_LAST_ENQUEUED_MS, 0L)
        if (now - last < MIN_INTERVAL_MS) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val data = workDataOf("endpoint" to endpoint, "api_key" to apiKey)

        val req = OneTimeWorkRequestBuilder<UploadMetricsWorker>()
            .setConstraints(constraints)
            .setInitialDelay(INITIAL_DELAY)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF)
            .setInputData(data)
            .addTag("metrics_upload_one_shot")
            .build()

        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "metrics_upload_one_shot",
            ExistingWorkPolicy.KEEP,
            req
        )
        p.edit { putLong(KEY_LAST_ENQUEUED_MS, now) }
    }

    /** Encola un envío inmediato (expedited con fallback). Ideal para gatillos por cota. */
    fun enqueueNowExpedited(ctx: Context, endpoint: String, apiKey: String) {
        if (endpoint.isBlank()) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = workDataOf("endpoint" to endpoint, "api_key" to apiKey)

        val req = OneTimeWorkRequestBuilder<UploadMetricsWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF)
            .setInputData(data)
            .addTag("metrics_upload_exit_flush")
            .build()

        // REPLACE para que, si se dispara varias veces, quede el último
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "metrics_upload_exit_flush",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    // --------- UMBRALES (cotas) ---------

    data class FlushThreshold(
        val appOpens: Int = 3,     // al registrar 3 aperturas, gatilla envío
        val tools: Int = 20,       // o 20 usos de herramientas
        val ads: Int = 20,         // o 20 impresiones de anuncios
        val total: Int? = null     // o umbral global (app+tools+ads). null = no usar
    )

    private suspend fun pendingDeltaTotals(ctx: Context): Triple<Int, Int, Int> {
        val repo = AggregatesRepository(ctx)
        val deltas = repo.buildDeltasSinceLastSent()
        val app = deltas.sumOf { it.appOpen }
        val tools = deltas.sumOf { it.tools.values.sum() }
        val ads = deltas.sumOf { it.ads.values.sum() }
        return Triple(app, tools, ads)
    }

    private suspend fun hasPendingPayload(ctx: Context): Boolean {
        val prefs = ctx.metricsDataStore.data.first()
        val json = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] ?: ""
        return json.isNotBlank()
    }

    /** Chequea umbrales y, si corresponde, encola envío inmediato (expedited). */
    suspend fun maybeFlushOnThreshold(ctx: Context, thresholds: FlushThreshold = FlushThreshold()) {
        val endpoint = UploadConfig.getEndpoint(ctx)
        val apiKey = UploadConfig.getApiKey(ctx)
        if (endpoint.isBlank()) return

        // Si ya hay payload congelado, enviarlo directamente
        if (hasPendingPayload(ctx)) {
            markDirty(ctx)
            enqueueNowExpedited(ctx, endpoint, apiKey)
            return
        }

        val (app, tools, ads) = pendingDeltaTotals(ctx)
        val total = app + tools + ads
        val trigger =
            app >= thresholds.appOpens ||
                    tools >= thresholds.tools ||
                    ads >= thresholds.ads ||
                    (thresholds.total != null && total >= thresholds.total)

        if (trigger) {
            markDirty(ctx)
            enqueueNowExpedited(ctx, endpoint, apiKey)
        }
    }
}
