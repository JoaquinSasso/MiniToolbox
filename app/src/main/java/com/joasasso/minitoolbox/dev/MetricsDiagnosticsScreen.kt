package com.joasasso.minitoolbox.dev

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.joasasso.minitoolbox.BuildConfig
import kotlinx.coroutines.launch

/**
 * Diagnóstico del pipeline de métricas.
 *
 * A diferencia de [MetricsDevScreen], esta pantalla está pensada para estar disponible en
 * release: es de sólo lectura y sus dos acciones (forzar envío, copiar diagnóstico) no
 * pueden destruir datos.
 */
@Composable
fun MetricsDiagnosticsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var health by remember { mutableStateOf<MetricsHealth?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadTick) {
        health = loadMetricsHealth(ctx)
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Diagnóstico de métricas", style = MaterialTheme.typography.titleLarge)

            val current = health
            if (current == null) {
                Text("Cargando…", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        summarizeHealth(current),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Estado", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text(
                        text = buildDiagnosticsText(current),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        triggerFlushNow(ctx)
                        Toast.makeText(ctx, "Envío encolado", Toast.LENGTH_SHORT).show()
                        scope.launch { reloadTick++ }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Forzar envío")
                }

                OutlinedButton(
                    onClick = { reloadTick++ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Actualizar")
                }
            }

            Button(
                onClick = {
                    copyToClipboard(ctx, buildDiagnosticsText(current))
                    Toast.makeText(ctx, "Diagnóstico copiado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copiar diagnóstico")
            }

            Text(
                "Este informe no contiene datos personales: sólo contadores y códigos de " +
                        "estado del envío de métricas anónimas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (BuildConfig.DEBUG) {
                HorizontalDivider()
                Text(
                    "Herramientas de desarrollo",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Las acciones destructivas sólo están disponibles en builds de debug.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MetricsDevScreen()
            }

            OutlinedButton(
                onClick = {
                    DevUnlock.setUnlocked(ctx, false)
                    Toast.makeText(ctx, "Diagnóstico bloqueado de nuevo", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ocultar esta pantalla")
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("MiniToolbox diagnóstico", text))
}