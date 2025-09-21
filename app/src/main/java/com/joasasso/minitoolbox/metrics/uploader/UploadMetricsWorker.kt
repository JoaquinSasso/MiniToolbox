package com.joasasso.minitoolbox.metrics.uploader

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class UploadMetricsWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    // Dentro de UploadMetricsWorker.kt

    private fun isValidDay(s: String): Boolean = Regex("""\d{4}-\d{2}-\d{2}""").matches(s)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = AggregatesRepository(applicationContext)
        val ds = applicationContext.metricsDataStore
        val prefs = ds.data.first()

        var batchId = prefs[MetricsKeys.PENDING_BATCH_ID].orEmpty()
        var payloadJson = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty()

        if (payloadJson.isBlank()) {
            val deltas = repo.buildDeltasSinceLastSent()
            if (deltas.isEmpty()) return@withContext Result.success()

            // Validación rápida de deltas
            for (d in deltas) {
                if (!isValidDay(d.day)) return@withContext Result.success() // ignora si raro
                if (d.appOpen < 0) return@withContext Result.success()
                if (d.tools.values.any { it < 0 }) return@withContext Result.success()
                if (d.ads.values.any { it < 0 }) return@withContext Result.success()
            }

            batchId = java.util.UUID.randomUUID().toString()

            val itemsArr = org.json.JSONArray()
            for (d in deltas) {
                val obj = org.json.JSONObject()
                    .put("day", d.day)
                    .put("app_open", d.appOpen)
                    .put("tools", org.json.JSONObject(d.tools as Map<*, *>))
                    .put("ads", org.json.JSONObject(d.ads as Map<*, *>))
                itemsArr.put(obj)
            }

            val body = org.json.JSONObject()
                .put("batch_id", batchId)
                .put("platform", "android")
                .put("app_version", safeVersionName())
                .put("items", itemsArr)

            payloadJson = body.toString()

            ds.edit { e ->
                e[MetricsKeys.PENDING_BATCH_ID] = batchId
                e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = payloadJson
            }
        }

        val endpoint = inputData.getString("endpoint") ?: return@withContext Result.failure()
        val apiKey = inputData.getString("api_key") ?: ""

        // Validación HTTPS
        if (!endpoint.startsWith("https://")) return@withContext Result.failure()

        val ok = postJson(endpoint, payloadJson, apiKey)
        if (ok) {
            repo.commitSentUpToCurrent()
            UploadScheduler.clearDirty(applicationContext)
            Result.success()
        } else {
            Result.retry()
        }
    }


    private fun postJson(endpoint: String, json: String, apiKey: String): Boolean {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) setRequestProperty("X-API-Key", apiKey)
        }
        return try {
            conn.outputStream.use { it.write(json.toByteArray()) }
            conn.responseCode in 200..299
        } catch (_: Throwable) {
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun safeVersionName(): String = try {
        val pm = applicationContext.packageManager
        val p = pm.getPackageInfo(applicationContext.packageName, 0)
        p.versionName ?: "unknown"
    } catch (_: Throwable) { "unknown" }

    companion object {
        fun testEnqueueNow(ctx: Context, endpoint: String, apiKey: String) {
            val data = workDataOf("endpoint" to endpoint, "api_key" to apiKey)
            androidx.work.WorkManager.getInstance(ctx).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<UploadMetricsWorker>()
                    .setInputData(data)
                    .build()
            )
        }
    }
}


