package com.joasasso.minitoolbox.dev

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.MobileAds
import com.joasasso.minitoolbox.utils.pro.CreditAccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MetricsDevScreen() {
    val ctx = LocalContext.current
    val activity = ctx as Activity
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var snapshot by remember { mutableStateOf<DevSnapshot?>(null) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val snap = loadDevSnapshot(ctx)
            withContext(Dispatchers.Main) {
                snapshot = snap
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp, top = 40.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Centro de Control de Métricas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Tarjeta de Diagnóstico / Credenciales
            snapshot?.let { s ->
                val cardBg = if (s.isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer
                val cardContent = if (s.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = cardBg,
                        contentColor = cardContent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (s.isEnabled) "● Métricas Habilitadas" else "✕ Métricas Deshabilitadas (Sin Keys)",
                            fontWeight = FontWeight.Bold,
                            color = cardContent,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Endpoint: ${s.endpoint.ifBlank { "(Vacío)" }}",
                            color = cardContent,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "API Key: ${s.apiKeyPreview}",
                            color = cardContent,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Estado Planificador: ${if (s.isDirty) "Dirty (Pendiente)" else "Limpio"}",
                            color = cardContent,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Deltas Pendientes: AppOpens=${s.remainingTotals.first} | Tools=${s.remainingTotals.second} | Ads=${s.remainingTotals.third}",
                            color = cardContent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Acciones de Prueba de Conexión
            Text(
                text = "Pruebas de Red y Envío",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isTesting = true
                        testResult = "Enviando ping a la Cloud Function…"
                        scope.launch {
                            val res = testDirectConnection(ctx)
                            testResult = res.fold(
                                onSuccess = { it },
                                onFailure = { "Error: ${it.message}" }
                            )
                            isTesting = false
                            refresh()
                        }
                    },
                    enabled = !isTesting
                ) {
                    Text("Test Conexión")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        triggerFlushNow(ctx)
                        Toast.makeText(ctx, "Upload forzado encolado en WorkManager", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            kotlinx.coroutines.delay(1200)
                            refresh()
                        }
                    }
                ) {
                    Text("Enviar Lote Ahora")
                }
            }

            testResult?.let { res ->
                val isSuccess = res.contains("OK")
                val resContainer = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                val resColor = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

                Surface(
                    color = resContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = res,
                        modifier = Modifier.padding(10.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = resColor
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Simulación de Eventos
            Text(
                text = "Simular Eventos Locales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = {
                    simulateMetricEvents(ctx, "open")
                    refresh()
                }) { Text("+1 AppOpen") }

                OutlinedButton(onClick = {
                    simulateMetricEvents(ctx, "test_tool")
                    refresh()
                }) { Text("+1 metricTest") }

                OutlinedButton(onClick = {
                    simulateMetricEvents(ctx, "metricsTest")
                    refresh()
                }) { Text("+1 metricsTest") }

                OutlinedButton(onClick = {
                    simulateMetricEvents(ctx, "ad")
                    refresh()
                }) { Text("+1 Ad") }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Utilidades generales
            Text(
                text = "Otras Utilidades",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { openAdInspector(activity) }
                ) { Text("Ad Inspector") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CreditAccessManager.endTimedPass(activity)
                        Toast.makeText(ctx, "Modo PRO finalizado", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Terminar PRO") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            sanitizeAndClearInvalidToolKeys(ctx)
                            Toast.makeText(ctx, "Claves inválidas purgadas", Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }
                ) {
                    Text("Sanitizar Tools")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            resetAllMetricsDataStore(ctx)
                            Toast.makeText(ctx, "DataStore reseteado a 0", Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }
                ) {
                    Text("Reset Total (0)")
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                onClick = { refresh() }
            ) {
                Text("Refrescar Datos de Pantalla")
            }

            // Visualizador de Payload JSON
            Text(
                text = "JSON de Lote Pendiente / Deltas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                val pending = snapshot?.pendingPayloadJson
                val textToShow = when {
                    !pending.isNullOrBlank() -> prettyJson(pending)
                    (snapshot?.remainingDeltas?.size ?: 0) > 0 -> {
                        // Reconstruir un preview visual limpio del lote que se armaría
                        val items = org.json.JSONArray()
                        snapshot?.remainingDeltas?.forEach { d ->
                            items.put(
                                org.json.JSONObject()
                                    .put("day", d.day)
                                    .put("app_open", d.appOpen)
                                    .put("tools", org.json.JSONObject(d.tools))
                                    .put("ads", org.json.JSONObject(d.ads))
                            )
                        }
                        prettyJson(
                            org.json.JSONObject()
                                .put("batch_id", "(preview)")
                                .put("items", items)
                                .toString()
                        )
                    }
                    else -> "(No hay datos acumulados pendientes de envío)"
                }

                Text(
                    text = textToShow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

fun openAdInspector(activity: Activity) {
    MobileAds.openAdInspector(activity) { error ->
        Log.d("Ads", "AdInspector abierto: ${error?.message}")
    }
}