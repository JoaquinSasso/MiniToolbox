// metrics/uploader/UploadScheduler.kt
package com.joasasso.minitoolbox.metrics.uploader

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration

object UploadScheduler {
    private const val SP_NAME = "metrics_upload_prefs"
    private const val KEY_DIRTY = "dirty"
    private const val KEY_LAST_ENQUEUED_MS = "last_enqueued_ms"

    private const val MIN_INTERVAL_MS = 3 * 60 * 60 * 1000L
    private val INITIAL_DELAY = Duration.ofSeconds(20)
    private val BACKOFF = Duration.ofMinutes(15)

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    fun markDirty(ctx: Context) {
        prefs(ctx).edit { putBoolean(KEY_DIRTY, true) }
    }
    fun clearDirty(ctx: Context) {
        prefs(ctx).edit { putBoolean(KEY_DIRTY, false) }
    }

    fun isDirty(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DIRTY, false)
    fun lastEnqueuedMs(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST_ENQUEUED_MS, 0L)

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
}
