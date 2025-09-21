package com.joasasso.minitoolbox.metrics.dev

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
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
import com.joasasso.minitoolbox.metrics.uploader.UploadConfig
import com.joasasso.minitoolbox.metrics.uploader.UploadMetricsWorker
import com.joasasso.minitoolbox.metrics.uploader.UploadScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MetricsDevScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var snap by remember { mutableStateOf<DevSnapshot?>(null) }
    var endpoint by remember { mutableStateOf(UploadConfig.getEndpoint(ctx)) }
    var apiKey by remember { mutableStateOf(UploadConfig.getApiKey(ctx)) }
    val scroll = rememberScrollState()

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            snap = loadDevSnapshot(ctx)
        }
    }


    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("Metrics Dev Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Config
        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            label = { Text("Endpoint URL (HTTPS)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key (header X-API-Key)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                UploadConfig.set(ctx, endpoint, apiKey)
                refresh()
            }) { Text("Guardar") }

            Button(onClick = {
                UploadScheduler.markDirty(ctx)
                UploadScheduler.maybeSchedule(ctx, endpoint, apiKey)
            }) { Text("Schedule") }

            Button(onClick = {
                UploadMetricsWorker.testEnqueueNow(ctx, endpoint, apiKey)
            }) { Text("Forzar") }
        }

        val dirty = UploadScheduler.isDirty(ctx)
        val lastMs = UploadScheduler.lastEnqueuedMs(ctx)
        Text("Dirty: $dirty  •  Last enqueued: ${if (lastMs == 0L) "-" else DateUtils.getRelativeTimeSpanString(lastMs)}")

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Button(onClick = { refresh() }) { Text("Refrescar") }

        // Resumen rápido
        snap?.let { s ->
            Text("App Open (por día): ${s.appOpenByDay}", color = MaterialTheme.colorScheme.onBackground)
            Text("Tools (por día): ${s.toolUseByDay}", color = MaterialTheme.colorScheme.onBackground)
            Text("Ads (por día): ${s.adImprByDay}", color = MaterialTheme.colorScheme.onBackground)

            Spacer(Modifier.height(8.dp))
            Text("Último enviado (app open): ${s.sentAppOpenByDay}", color = MaterialTheme.colorScheme.onBackground)
            Text("Último enviado (tools): ${s.sentToolUseByDay}", color = MaterialTheme.colorScheme.onBackground)
            Text("Último enviado (ads): ${s.sentAdImprByDay}", color = MaterialTheme.colorScheme.onBackground)

            Spacer(Modifier.height(8.dp))
            Text("Pending batch id: ${s.pendingBatchId.ifBlank { "-" }}", color = MaterialTheme.colorScheme.onBackground)
            Text("Pending payload:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (s.pendingPayloadJson.isBlank()) "(ninguno)"
                    else prettyJson(s.pendingPayloadJson),
                    modifier = Modifier.padding(12.dp)
                )
            }
        } ?: Text("Cargando snapshot…")
    }
}
