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
import java.util.UUID

class UploadMetricsWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private fun isValidDay(s: String): Boolean = Regex("""\d{4}-\d{2}-\d{2}""").matches(s)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = AggregatesRepository(applicationContext)
        val ds = applicationContext.metricsDataStore
        val prefs = ds.data.first()

        var batchId = prefs[MetricsKeys.PENDING_BATCH_ID].orEmpty()
        var payloadJson = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty()
        var deltasForCommit: List<AggregatesRepository.DayDelta>? = null

        // Si no hay payload pendiente, construimos deltas y congelamos JSON para idempotencia
        if (payloadJson.isBlank()) {
            val deltas = repo.buildDeltasSinceLastSent()
            if (deltas.isEmpty()) return@withContext Result.success()

            // Validación rápida de deltas
            for (d in deltas) {
                if (!isValidDay(d.day)) return@withContext Result.success()
                if (d.appOpen < 0) return@withContext Result.success()
                if (d.tools.values.any { it < 0 }) return@withContext Result.success()
                if (d.ads.values.any { it < 0 }) return@withContext Result.success()
                if (d.versions.values.any { it < 0 }) return@withContext Result.success()
                if (d.versionsFirstSeen.values.any { it < 0 }) return@withContext Result.success()
                if (d.langPrimary.values.any { it < 0 }) return@withContext Result.success()
                if (d.langSecondary.values.any { it < 0 }) return@withContext Result.success()
                if (d.widgets.values.any { it < 0 }) return@withContext Result.success()
            }

            batchId = UUID.randomUUID().toString()

            val itemsArr = org.json.JSONArray()
            for (d in deltas) {
                val obj = org.json.JSONObject()
                    .put("day", d.day)
                    .put("app_open", d.appOpen)
                    .put("tools", org.json.JSONObject(d.tools as Map<*, *>))
                    .put("ads", org.json.JSONObject(d.ads as Map<*, *>))
                    // NUEVOS campos:
                    .put("versions", org.json.JSONObject(d.versions as Map<*, *>))                // DAU por versión
                    .put("versions_first_seen", org.json.JSONObject(d.versionsFirstSeen as Map<*, *>))
                    .put("lang_primary", org.json.JSONObject(d.langPrimary as Map<*, *>))
                    .put("lang_secondary", org.json.JSONObject(d.langSecondary as Map<*, *>))
                    .put("widgets", org.json.JSONObject(d.widgets as Map<*, *>))
                itemsArr.put(obj)
            }

            val body = org.json.JSONObject()
                .put("batch_id", batchId)
                .put("platform", "android")
                .put("app_version", safeVersionName())
                .put("items", itemsArr)

            payloadJson = body.toString()
            deltasForCommit = deltas

            ds.edit { e ->
                e[MetricsKeys.PENDING_BATCH_ID] = batchId
                e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = payloadJson
            }
        } else {
            // Si ya hay pending, parseamos sus deltas para hacer commit correcto tras el 2xx
            deltasForCommit = parseDeltasFromPendingPayload(payloadJson)
        }

        val endpoint = inputData.getString("endpoint") ?: return@withContext Result.failure()
        val apiKey = inputData.getString("api_key") ?: ""

        // Solo HTTPS
        if (!endpoint.startsWith("https://")) return@withContext Result.failure()

        val ok = postJson(endpoint, payloadJson, apiKey)
        if (ok) {
            // Commit SOLO de lo enviado en este batch
            val toCommit = deltasForCommit
            repo.commitSent(toCommit)

            // ¿Queda algo nuevo para enviar? (ocurrió mientras el batch estaba pendiente)
            val remaining = repo.buildDeltasSinceLastSent()
            if (remaining.isNotEmpty()) {
                UploadScheduler.markDirty(applicationContext)
                UploadScheduler.maybeSchedule(applicationContext, endpoint, apiKey)
            } else {
                UploadScheduler.clearDirty(applicationContext)
            }

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

    /** Reconstruye la lista de deltas (por día) a partir del JSON de payload pendiente. */
    private fun parseDeltasFromPendingPayload(json: String): List<AggregatesRepository.DayDelta> {
        return try {
            val root = org.json.JSONObject(json)
            val items = root.optJSONArray("items") ?: return emptyList()
            val out = mutableListOf<AggregatesRepository.DayDelta>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val day = item.optString("day", "")
                if (!isValidDay(day)) continue

                val app = item.optInt("app_open", 0)

                fun objToMap(obj: org.json.JSONObject?): MutableMap<String, Int> {
                    val o = obj ?: org.json.JSONObject()
                    val m = mutableMapOf<String, Int>()
                    val it = o.keys()
                    while (it.hasNext()) {
                        val k = it.next()
                        m[k] = o.optInt(k, 0)
                    }
                    return m
                }

                val toolsMap    = objToMap(item.optJSONObject("tools"))
                val adsMap      = objToMap(item.optJSONObject("ads"))
                val verMap      = objToMap(item.optJSONObject("versions"))
                val verFsMap    = objToMap(item.optJSONObject("versions_first_seen"))
                val langPMap    = objToMap(item.optJSONObject("lang_primary"))
                val langSMap    = objToMap(item.optJSONObject("lang_secondary"))
                val widgetsMap  = objToMap(item.optJSONObject("widgets"))

                out += AggregatesRepository.DayDelta(
                    day = day,
                    appOpen = app,
                    tools = toolsMap,
                    ads = adsMap,
                    versions = verMap,
                    versionsFirstSeen = verFsMap,
                    langPrimary = langPMap,
                    langSecondary = langSMap,
                    widgets = widgetsMap
                )
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

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
