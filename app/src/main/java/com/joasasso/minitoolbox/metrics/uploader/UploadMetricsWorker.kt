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
        var deltasForCommit: List<AggregatesRepository.DayDelta>? = null


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

            deltasForCommit = deltas

            ds.edit { e ->
                e[MetricsKeys.PENDING_BATCH_ID] = batchId
                e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = payloadJson
            }
        }
            else {
                // 🔹 hay un payload pendiente: parsear sus deltas para el commit
                deltasForCommit = parseDeltasFromPendingPayload(payloadJson)
            }

        val endpoint = inputData.getString("endpoint") ?: return@withContext Result.failure()
        val apiKey = inputData.getString("api_key") ?: ""

        // Validación HTTPS
        if (!endpoint.startsWith("https://")) return@withContext Result.failure()

        val ok = postJson(endpoint, payloadJson, apiKey)
        if (ok) {
            // Commit SOLO de lo que se envió efectivamente en este batch
            val toCommit = deltasForCommit
            repo.commitSent(toCommit)

            // ¿Quedaron más deltas? (ocurrieron durante la “ventana pendiente”)
            val remaining = repo.buildDeltasSinceLastSent()
            if (remaining.isNotEmpty()) {
                // Seguir enviando automáticamente
                UploadScheduler.markDirty(applicationContext)
                val endpoint2 = endpoint
                val apiKey2 = apiKey
                UploadScheduler.maybeSchedule(applicationContext, endpoint2, apiKey2)
            } else {
                // Nada más por enviar
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

                val toolsObj = item.optJSONObject("tools") ?: org.json.JSONObject()
                val toolsMap = mutableMapOf<String, Int>()
                val itTools = toolsObj.keys()
                while (itTools.hasNext()) {
                    val k = itTools.next()
                    toolsMap[k] = toolsObj.optInt(k, 0)
                }

                val adsObj = item.optJSONObject("ads") ?: org.json.JSONObject()
                val adsMap = mutableMapOf<String, Int>()
                val itAds = adsObj.keys()
                while (itAds.hasNext()) {
                    val k = itAds.next()
                    adsMap[k] = adsObj.optInt(k, 0)
                }

                out += AggregatesRepository.DayDelta(day, app, toolsMap, adsMap)
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

}




