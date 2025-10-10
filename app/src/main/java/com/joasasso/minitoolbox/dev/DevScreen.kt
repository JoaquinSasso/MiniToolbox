package com.joasasso.minitoolbox.dev

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.MobileAds
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun MetricsDevScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var state by remember {
        mutableStateOf(PreviewState(mode = PreviewMode.Loading, prettyJson = "(cargando…)", summary = ""))
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            state = buildNextBatchPreview(ctx)
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp, top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Metrics Dev – Próximo lote a enviar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            when (state.mode) {
                PreviewMode.Frozen -> "Estado: Próximo lote CONGELADO (pendiente de envío)"
                PreviewMode.Preview -> "Estado: Vista previa (si se enviara ahora)"
                PreviewMode.Empty -> "Estado: No hay datos para enviar"
                PreviewMode.Loading -> "Estado: Cargando…"
            },
            color = MaterialTheme.colorScheme.onBackground
        )

        Button(onClick = { openAdInspector(ctx as Activity) }) { Text("Abrir Ad Inspector") }

        Button(onClick = { refresh() }) { Text("Refrescar") }


        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

//        if (state.summary.isNotBlank()) {
//            Text("Resumen por día", style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.SemiBold,
//                color = MaterialTheme.colorScheme.onBackground)
//            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
//                Text(text = state.summary, modifier = Modifier.padding(12.dp))
//            }
//            Spacer(Modifier.height(8.dp))
//        }

        Text("Payload JSON", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground)
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Text(text = state.prettyJson, modifier = Modifier.padding(12.dp))
        }
    }
}

// ------------------- Helpers / Modelo de vista -------------------

private enum class PreviewMode { Frozen, Preview, Empty, Loading }

private data class PreviewState(
    val mode: PreviewMode,
    val prettyJson: String,
    val summary: String
)

private suspend fun buildNextBatchPreview(ctx: Context): PreviewState {
    // 1) Si hay payload pendiente, ese ES el próximo lote
    val prefs = ctx.metricsDataStore.data.first()
    val frozenJson = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty()
    if (frozenJson.isNotBlank()) {
        val pretty = prettyJsonDevScreen(frozenJson)
        val summary = summarizePayload(frozenJson)
        return PreviewState(PreviewMode.Frozen, pretty, summary)
    }

    // 2) Si no hay pendiente, calcular deltas actuales y armar preview
    val repo = AggregatesRepository(ctx)
    val deltas = repo.buildDeltasSinceLastSent()
    if (deltas.isEmpty()) {
        return PreviewState(PreviewMode.Empty, "(no hay datos para enviar)", "")
    }

    val items = JSONArray()
    deltas.forEach { d ->
        val obj = JSONObject()
            .put("day", d.day)
            .put("app_open", d.appOpen)
            .put("tools", JSONObject(d.tools as Map<*, *>))
            .put("ads", JSONObject(d.ads as Map<*, *>))
            // NUEVOS campos:
            .put("versions", JSONObject(d.versions as Map<*, *>))                    // DAU por versión
            .put("versions_first_seen", JSONObject(d.versionsFirstSeen as Map<*, *>))
            .put("lang_primary", JSONObject(d.langPrimary as Map<*, *>))
            .put("lang_secondary", JSONObject(d.langSecondary as Map<*, *>))
            .put("widgets", JSONObject(d.widgets as Map<*, *>))
        items.put(obj)
    }

    val body = JSONObject()
        .put("batch_id", "(preview)")                 // marcador solo visual
        .put("platform", "android")
        .put("app_version", safeVersionName(ctx))
        .put("items", items)

    val json = body.toString()
    val pretty = prettyJson(json)
    val summary = summarizePayload(json)

    return PreviewState(PreviewMode.Preview, pretty, summary)
}

private fun safeVersionName(ctx: Context): String = try {
    val pm = ctx.packageManager
    val p = pm.getPackageInfo(ctx.packageName, 0)
    p.versionName ?: "unknown"
} catch (_: Throwable) { "unknown" }

private fun prettyJsonDevScreen(json: String): String {
    return try {
        if (json.isBlank()) return "(vacío)"
        val any = JSONObject(json)
        any.toString(2)
    } catch (_: Throwable) {
        try {
            val arr = JSONArray(json)
            arr.toString(2)
        } catch (_: Throwable) {
            json
        }
    }
}

/** Crea un resumen legible por día con totales de cada rubro (incluye nuevas métricas). */
private fun summarizePayload(json: String): String = try {
    val root = JSONObject(json)
    val items = root.optJSONArray("items") ?: return ""
    val sb = StringBuilder()
    for (i in 0 until items.length()) {
        val it = items.getJSONObject(i)
        val day = it.optString("day", "-")
        sb.appendLine("• $day")
        fun sum(objName: String): Int {
            val o = it.optJSONObject(objName) ?: return 0
            var s = 0
            val k = o.keys()
            while (k.hasNext()) {
                val key = k.next()
                s += o.optInt(key, 0)
            }
            return s
        }
        val app = it.optInt("app_open", 0)
        val tools = sum("tools")
        val ads = sum("ads")
        val ver = sum("versions")
        val vfs = sum("versions_first_seen")
        val lp = sum("lang_primary")
        val ls = sum("lang_secondary")
        val w = sum("widgets")

        sb.appendLine("   app_open=$app  tools=$tools  ads=$ads  versions=$ver  first_seen=$vfs  langP=$lp  langS=$ls  widgets=$w")
    }
    sb.toString().trimEnd()
} catch (_: Throwable) { "" }

//Ad Inspector
fun openAdInspector(activity: Activity) {
    MobileAds.openAdInspector(activity) { error ->
        // Si error == null, se abrió bien
        Log.d("Ads", "AdInspector abierto: ${error?.message}")
    }
}